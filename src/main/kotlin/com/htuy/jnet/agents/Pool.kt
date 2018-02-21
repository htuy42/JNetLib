package com.htuy.jnet.agents

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Queues
import com.htuy.jnet.messages.*
import com.htuy.jnet.modules.ModuleManager
import com.htuy.jnet.modules.SiteInstaller
import com.htuy.jnet.protocol.Protocol
import com.htuy.jnet.protocol.ProtocolBuilder
import com.htuy.jnet.protocol.STANDARD_CLIENT_PROTOCOL
import com.htuy.jnet.protocol.STANDARD_WORKER_PROTOCOL
import com.htuy.kt.stuff.LOGGER
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import sun.awt.Mutex
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

data class WorkPair(val block: WorkBlock,
                    val owner: Channel)

fun WorkerRequestHandlersFactory(pool: Pool): () -> ConnectionManager {
    return {
        ConnectionManager(listOf(
                MessageTypeFunHandler(SubWorkMessage::class.java, { ctx, msg ->
                    LOGGER.trace { "Received work done message in handler. Sending to pool." }
                    pool.handleWorkerDone(ctx.channel().id(), msg.subWork)
                }),
                MessageTypeFunHandler(WorkerPowerMessage::class.java, { ctx, msg ->
                    LOGGER.debug {
                        "Got update regarding workers ability to handle work \\ threadedness. " +
                                "\nWorker can handle ${msg.amount} jobs at once."
                    }
                    pool.workerPower[ctx.channel().id()] = msg.amount
                })))
    }
}

fun ClientRequestHandlersFactory(pool: Pool): () -> ConnectionManager {
    return {
        ConnectionManager(listOf(
                MessageTypeFunHandler(WorkMessage::class.java, { ctx, msg ->
                    LOGGER.trace { "Received work request in handler. Sending to pool." }
                    pool.handleNewWorkRequest(ctx, msg.work)
                }),
                MessageTypeFunHandler(ModuleMessage::class.java, { ctx, msg ->
                    LOGGER.trace { "Received module install request in handler. Sending to pool." }
                    pool.handleModuleRequest(msg)
                }),
                MessageTypeFunHandler(LifecycleMessage::class.java, { ctx, msg ->
                    when (msg.event) {
                        LifecycleEvent.SHUTDOWN -> pool.shutdown()
                    }
                })))
    }
}


class Pool(val workerPort: Int,
           val clientPort: Int,
           val heartbeatFrequencyClient: Int = -1,
           val heartbeatFrequencyWorkers: Int = -1,
           val workerProtocol: Protocol = STANDARD_WORKER_PROTOCOL,
           val clientProtocol: Protocol = STANDARD_CLIENT_PROTOCOL) {


    //todo fine grained locking on the actual collections
    val workers = ConcurrentHashMap<ChannelId, Channel>()
    val workerPower = ConcurrentHashMap<ChannelId, Int>()
    val freeWorkers = Queues.newConcurrentLinkedQueue<ChannelId>()
    val workInProgress: Multimap<ChannelId, WorkSubunit> = MultimapBuilder.hashKeys().hashSetValues().build()
    val workNotAssigned = Queues.newConcurrentLinkedQueue<WorkSubunit>()
    val workDone = Collections.synchronizedList(ArrayList<WorkSubunit>())
    val workQueue = Queues.newConcurrentLinkedQueue<WorkPair>()
    var currentWorkPair: WorkPair? = null
    val lock: Mutex = Mutex()
    val requestedModules = Collections.synchronizedSet(HashSet<String>())
    val manager = ModuleManager("modules/", SiteInstaller())
    var workerServer: Server? = null
    var clientServer: Server? = null

    fun WorkerDeathFactory(): (ChannelHandlerContext) -> Unit {
        return {
            LOGGER.warn { "Worker disconnected from the pool" }
            val id = it.channel()
                    .id()
            lock.lock()
            workers.remove(it.channel().id(), it.channel())
            workNotAssigned.addAll(workInProgress.get(id))
            workInProgress.removeAll(id)
            lock.unlock()
            notifyWorkAvailable()
        }
    }

    fun WorkerLifeFactory(): (ChannelHandlerContext) -> Unit {
        return {
            LOGGER.warn { "New worker connected to the pool." }
            installCurrentModulesToWorker(it.channel())
            workers[it.channel().id()] = it.channel()
            workerPower[it.channel().id()] = 1
            assignWorkIfAvailable(it.channel().id())
        }
    }


    fun launch() {
        LOGGER.debug { "Launching pool" }
        workerServer = Server(workerPort, workerProtocol,
                              WorkerRequestHandlersFactory(this)
        )
        if (heartbeatFrequencyWorkers != -1) {
            workerServer?.installAfter("decoder",
                                       "heartbeat",
                                       { HeartbeatMonitor(heartbeatFrequencyWorkers) })
        }

        clientServer = Server(clientPort, clientProtocol,ClientRequestHandlersFactory(this))
        if (heartbeatFrequencyClient != -1) {
            workerServer?.installAfter("decoder",
                                       "heartbeat",
                                       { HeartbeatMonitor(heartbeatFrequencyClient) })
        }

        val workerFuture = workerServer?.connect() ?: throw IllegalStateException("Worker server startup problem")
        val clientFuture = clientServer?.connect() ?: throw IllegalStateException("Client server startup problem")
        LOGGER.debug { "Pool launched. Sleeping on server close futures" }
        workerFuture.channel()
                .closeFuture()
                .sync()
        LOGGER.debug { "Worker server shutdown" }
        clientFuture.channel()
                .closeFuture()
                .sync()
        LOGGER.debug { "Client server shutdown" }
    }

    fun handleModuleRequest(msg: ModuleMessage) {
        lock.lock()
        LOGGER.debug { "Received request to install modules ${msg.moduleNames.joinToString(separator = " ")}" }
        manager.loadAsNeeded(msg.moduleNames, msg.update)
        for (module in msg.moduleNames) {
            if (!requestedModules.contains(module)) {
                LOGGER.warn { "Installing new module $module" }
                requestedModules.add(module)
                for (workChannel in workers.values) {
                    workChannel.writeAndFlush(ModuleMessage(listOf(module), msg.update))
                }
            }
        }
        lock.unlock()
    }

    fun installCurrentModulesToWorker(channel: Channel) {
        LOGGER.debug { "Making worker install all currently required modules." }
        lock.lock()
        for (module in requestedModules) {
            channel.writeAndFlush(ModuleMessage(listOf(module)))
        }
        lock.unlock()
    }

    fun handleWorkerDone(id: ChannelId,
                         workSubunit: WorkSubunit) {
        LOGGER.trace { "Worker ${id.asShortText()} finished work unit" }
        lock.lock()
        workDone.add(workSubunit)
        workInProgress.remove(id, workSubunit)
        lock.unlock()
        handleAllDoneCheck()
        assignWorkIfAvailable(id)
    }

    fun handleAllDoneCheck() {
        LOGGER.trace { "Performing all done check." }
        lock.lock()
        var newPair = false
        if (workNotAssigned.isEmpty() && workInProgress.isEmpty() && currentWorkPair != null) {
            LOGGER.warn { "Finished current job. Sending reply!" }
            val currentWorkResponse = currentWorkPair?.block?.recombineWork(workDone)
                    ?: throw IllegalStateException("Issue with concurreny in all done check")
            currentWorkPair?.owner?.writeAndFlush(currentWorkResponse)
                    ?: throw IllegalStateException("Issue with concurreny in all done check")
            workDone.clear()
            currentWorkPair = workQueue.poll()
            if (currentWorkPair != null) {
                val newWork = currentWorkPair?.block?.splitWork()
                        ?: throw IllegalArgumentException("Work split to null list")
                workNotAssigned.addAll(newWork)
                newPair = true
            }
        }
        lock.unlock()
        if (newPair) {
            notifyWorkAvailable()
        }
    }


    fun notifyWorkAvailable() {
        LOGGER.trace { "Notifying work available (usually because it was freed by a death or a new job started)" }
        lock.lock()
        while (!freeWorkers.isEmpty() && !workNotAssigned.isEmpty()) {
            val id = freeWorkers.poll()
            assignWorkIfAvailable(id, false)
        }
        lock.unlock()
    }

    private fun assignWork(id: ChannelId,
                           work: WorkSubunit) {
        LOGGER.trace { "Performing work assign for ${id.asShortText()}" }
        workInProgress.put(id, work)
        workers[id]?.writeAndFlush(SubWorkMessage(work)) ?: throw IllegalStateException("Tried to assign work to " +
                                                                                                "a worker that doesn't exist")
    }

    fun assignWorkIfAvailable(id: ChannelId, getLock: Boolean = true) {
        if (getLock) {
            lock.lock()
        }
        LOGGER.trace { "Checking work assign for ${id.asShortText()}" }
        while (workerPower[id]!! > workInProgress.get(id).size) {
            val todo: WorkSubunit? = workNotAssigned.poll()
            if (todo != null) {
                assignWork(id, todo)
            } else {
                LOGGER.warn { "Worker ${id.asShortText()} not getting work because none was available" }
                break
            }
        }
        if (workerPower[id]!! > workInProgress.get(id).size) {
            freeWorkers.add(id)
        }
        if (getLock) {
            lock.unlock()
        }
    }

    private fun assertNothingDoing() {
        if (workNotAssigned.size != 0 || !workInProgress.isEmpty || currentWorkPair != null) {
            throw IllegalStateException("Problem with current state")
        }
    }

    fun handleNewWorkRequest(ctx: ChannelHandlerContext,
                             work: WorkBlock) {
        lock.lock()
        LOGGER.debug("Got new work request of types ${work.modulesRequired.joinToString(" ")}")
        if (currentWorkPair == null) {
            LOGGER.debug { "Was free, so using as current work." }
            assertNothingDoing()
            currentWorkPair = WorkPair(work, ctx.channel())
            workNotAssigned.addAll(work.splitWork())
            lock.unlock()
            notifyWorkAvailable()
            return
        } else {
            LOGGER.debug { "Was busy, so adding to queue." }
            workQueue.add(WorkPair(work, ctx.channel()))
        }
        lock.unlock()
        handleAllDoneCheck()
    }

    fun shutdown() {
        lock.lock()
        LOGGER.warn { "Pool shutting down at remote request." }
        for (worker in workers.values) {
            worker.writeAndFlush(LifecycleMessage(LifecycleEvent.SHUTDOWN))
        }
        val message = ErrorMessage(ErrorType.SHUTTING_DOWN, "Had to shutdown. Work request will not be serviced.")
        for (clientPair in workQueue) {
            clientPair.owner.writeAndFlush(message)
        }
        currentWorkPair?.owner?.writeAndFlush(message)
        workerServer?.shutdown()
        clientServer?.shutdown()
        System.exit(0)
    }

}