package com.htuy.jnet.agents

import com.google.common.collect.Queues
import com.htuy.jnet.messages.*
import com.htuy.jnet.modules.ModuleManager
import com.htuy.jnet.modules.SiteInstaller
import com.htuy.kt.stuff.Factory
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

class DoneWithWorkHandlerFactory(val pool: Pool) : Factory<List<MessageHandler>> {
    override fun getInstance(): List<MessageHandler> {
        return listOf(MessageTypeFunHandler(SubWorkMessage::class.java, { ctx, msg ->
            LOGGER.trace { "Received work done message in handler. Sending to pool." }
            pool.handleWorkerDone(ctx.channel().id(), msg.subWork)
        }))
    }
}

class ClientRequestHandlersFactory(val pool: Pool) : Factory<List<MessageHandler>> {
    override fun getInstance(): List<MessageHandler> {
        return listOf(
                MessageTypeFunHandler(WorkMessage::class.java, { ctx, msg ->
                    LOGGER.trace { "Received work request in handler. Sending to pool." }
                    pool.handleNewWorkRequest(ctx, msg.work)
                }),
                MessageTypeFunHandler(ModuleMessage::class.java, { ctx, msg ->
                    LOGGER.trace { "Received module install request in handler. Sending to pool." }
                    pool.handleModuleRequest(msg)
                }))
    }
}


class Pool(val workerPort: Int,
           val clientPort: Int) {
    //todo fine grained locking on the actual collections
    val workers = ConcurrentHashMap<ChannelId, Channel>()
    val freeWorkers = Queues.newConcurrentLinkedQueue<ChannelId>()
    val workInProgress = ConcurrentHashMap<ChannelId, WorkSubunit>()
    val workNotAssigned = Queues.newConcurrentLinkedQueue<WorkSubunit>()
    val workDone = Collections.synchronizedList(ArrayList<WorkSubunit>())
    val workQueue = Queues.newConcurrentLinkedQueue<WorkPair>()
    var currentWorkPair: WorkPair? = null
    val lock: Mutex = Mutex()
    val requestedModules = Collections.synchronizedSet(HashSet<String>())
    val manager = ModuleManager("moudles/", SiteInstaller())

    inner class WorkerDeathFactory : Factory<LifecycleHandler> {
        override fun getInstance(): LifecycleHandler {
            return LambdaLifecycleHandler({
                                              LOGGER.warn { "Worker disconnected from the pool" }
                                              val id = it.channel()
                                                      .id()
                                              lock.lock()
                                              workers.remove(it.channel().id(), it.channel())
                                              workNotAssigned.add(workInProgress.get(id))
                                              workInProgress.remove(id)
                                              lock.unlock()
                                              notifyWorkAvailable()
                                          })
        }
    }

    inner class WorkerLifeFactory : Factory<LifecycleHandler> {
        override fun getInstance(): LifecycleHandler {
            return LambdaLifecycleHandler({
                                              LOGGER.warn { "New worker connected to the pool." }
                                              installCurrentModulesToWorker(it.channel())
                                              workers[it.channel().id()] = it.channel()
                                              assignWorkIfAvailable(it.channel().id())
                                          })
        }
    }

    fun launch() {
        LOGGER.debug { "Launching pool" }
        val workerServer = Server(workerPort,
                                  DoneWithWorkHandlerFactory(this),
                                  WorkerLifeFactory(),
                                  WorkerDeathFactory())
        val clientServer = Server(clientPort, ClientRequestHandlersFactory(this))

        val workerFuture = workerServer.connect()
        val clientFuture = clientServer.connect()
        LOGGER.debug { "Pool launched. Sleeping on server close futures" }
        workerFuture.channel()
                .closeFuture()
                .sync()
        clientFuture.channel()
                .closeFuture()
                .sync()
    }

    fun handleModuleRequest(msg: ModuleMessage) {
        lock.lock()
        LOGGER.debug { "Received request to install modules ${msg.moduleNames.joinToString(separator = " ")}" }
        manager.loadAsNeeded(msg.moduleNames)
        for (module in msg.moduleNames) {
            if (!requestedModules.contains(module)) {
                LOGGER.warn { "Installing new module $module" }
                requestedModules.add(module)
                for (workChannel in workers.values) {
                    workChannel.writeAndFlush(ModuleMessage(listOf(module)))
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
        workInProgress.remove(id)
        lock.unlock()
        handleAllDoneCheck()
        assignWorkIfAvailable(id)
    }

    fun handleAllDoneCheck() {
        LOGGER.debug { "Performing all done check." }
        lock.lock()
        var newPair = false
        if (workNotAssigned.isEmpty() && workInProgress.isEmpty() && currentWorkPair != null) {
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
            notifyNewWorkPair()
        }
    }

    fun notifyNewWorkPair() {
        LOGGER.debug { "Notifying new pair." }
        lock.lock()
        var curSize = Math.min(freeWorkers.size, workNotAssigned.size)
        while (!freeWorkers.isEmpty() && curSize > 0) {
            val id = freeWorkers.poll()
            val todo: WorkSubunit? = workNotAssigned.poll()

            if (todo != null) {
                assignWork(id, todo)
            } else {
                freeWorkers.add(id)
                break
            }
            curSize--
        }
        lock.unlock()
    }

    fun notifyWorkAvailable() {
        LOGGER.trace { "Notifying work available (usually because it was freed by a death)" }
        lock.lock()
        val freeWorker = freeWorkers.poll()
        if (freeWorker != null) {
            val todo = workNotAssigned.poll()
            if (todo != null) {
                assignWork(freeWorker, todo)
            } else {
                freeWorkers.add(freeWorker)
            }
        }
        lock.unlock()
    }

    private fun assignWork(id: ChannelId,
                           work: WorkSubunit) {
        LOGGER.trace { "Performing work assign for ${id.asShortText()}" }
        workers[id]?.writeAndFlush(SubWorkMessage(work)) ?: throw IllegalStateException("Tried to assign work to " +
                                                                                                "a worker that doesn't exist")
    }

    fun assignWorkIfAvailable(id: ChannelId) {
        lock.lock()
        LOGGER.trace { "Checking work assign for ${id.asShortText()}" }
        val todo: WorkSubunit? = workNotAssigned.poll()
        if (todo != null) {
            assignWork(id, todo)
        } else {
            LOGGER.warn { "Worker ${id.asShortText()} not getting work because none was available" }
            freeWorkers.add(id)
        }
        lock.unlock()
    }

    private fun assertNothingDoing() {
        if (workNotAssigned.size != 0 || workInProgress.size != 0 || currentWorkPair != null) {
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
            notifyNewWorkPair()
            return
        } else {
            LOGGER.debug { "Was busy, so adding to queue." }
            workQueue.add(WorkPair(work, ctx.channel()))
        }
        lock.unlock()
        handleAllDoneCheck()
    }

}