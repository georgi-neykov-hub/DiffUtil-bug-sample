package com.example.diffutilbug

import androidx.core.util.Pools
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil_Fixed3
import androidx.recyclerview.widget.ListUpdateCallback
import com.example.diffutilbug.ReportingListUpdateCallback.DiffOperation.Companion.change
import com.example.diffutilbug.ReportingListUpdateCallback.DiffOperation.Companion.insert
import com.example.diffutilbug.ReportingListUpdateCallback.DiffOperation.Companion.move
import com.example.diffutilbug.ReportingListUpdateCallback.DiffOperation.Companion.remove
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList

class ListUpdateCallbackTest {

    private val logger: (String) -> Unit = { print(it) }

    @Test
    fun test_empty_list_to_Non_Empty_List() {
        testDiffForItems('C'..'E')
    }

    @Test
    fun test_non_empty_list_to_empty_List() {
        testDiffForItems('A'..'D', emptyList())
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_1() {
        testDiffForItems('C'..'E', 'A'..'H')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_2() {
        testDiffForItems('A'..'D', 'A'..'H')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_3() {
        testDiffForItems('A'..'D', 'A'..'H')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_4() {
        testDiffForItems('A'..'C', ('A'..'C').reversed())
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_5() {
        testDiffForItems('A'..'H', 'C'..'E')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_6() {
        testDiffForItems('B'..'C', 'A'..'D')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_7() {
        testDiffForItems("CDEFIJKL", "ABCDEFGHIKLMN")
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_8() {
        testDiffForItems("CGHIK".toList(), 'A'..'I')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_9() {
        testDiffForItems('A'..'D', ('A'..'E').reversed())
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_10() {
        testDiffForItems('A'..'D', ('A'..'C').reversed())
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_11() {
        testDiffForItems('B'..'D', ('A'..'D').reversed())
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_12() {
        testDiffForItems('A'..'D', ('A'..'D').reversed().plus('E'..'F'))
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_13() {
        testDiffForItems("BDF".toList(), 'A'..'G')
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_14() {
        testDiffForItems('A'..'D', ('A'..'D').reversed().plus('E'))
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_15() {
        testDiffForItems('A'..'D', ('A'..'D').reversed().plus('E'..'G'))
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_16() {
        testDiffForItems("BDF".toList(), ('A'..'D').plus('F'..'M'))
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_17() {
        testDiffForItems('A'..'C', 'A'..'G', ('1'..'5').plus("FFGHQRXYZ".toList()))
    }

    private fun testDiffForItems(vararg lists: CharSequence) =
        this.testDiffForItems(*lists.map { it.toList() }.toTypedArray())

    private fun testDiffForItems(vararg lists: Iterable<Char>) {
        val state = mutableListOf<Char>()
        lists.forEach {
            diffList(state, it.toList())
        }
    }


    private fun diffList(currentList: MutableList<Char>, nextList: List<Char>) {
        logger("\nSubmit list:\n Old: $currentList\n New: ${nextList}\n\n")

        logger("\nScript without move detection:\n\n")
        val stateCopy = currentList.toMutableList()
        stateCopy.patchTo(nextList, detectMoves = false)
        assertConsistency(stateCopy, nextList)

        logger("\nScript with move detection:\n\n")
        currentList.patchTo(nextList)
        assertConsistency(currentList, nextList)
        assertConsistency(stateCopy, currentList)
    }

    private fun <T> assertConsistency(actualList: List<T>, nextList: List<T>) {

        if (nextList.size != actualList.size) {
            Assert.assertEquals(
                "Item count mismatch, expected (${nextList.size}), but was (${actualList.size}).",
                nextList, actualList
            )
        }
        for (position in nextList.indices) {
            val expectedItem = nextList[position]
            val actualItem = actualList[position]
            if (actualItem != expectedItem) {
                Assert.assertEquals(
                    "Inconsistency at ($position), expected `$expectedItem`, but was '$actualItem'.",
                    nextList, actualList
                )
            }
        }
    }

}

private val DEFAULT_DIFF_ITEM_CALLBACK = object : DiffUtil.ItemCallback<Any?>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any) = oldItem == newItem
    override fun areContentsTheSame(oldItem: Any, newItem: Any) = true
}

@Suppress("UNCHECKED_CAST")
private fun <T> defaultItemCallback(): DiffUtil.ItemCallback<T> =
    DEFAULT_DIFF_ITEM_CALLBACK as DiffUtil.ItemCallback<T>

fun <T> MutableList<T>.patchTo(
    nextList: List<T>,
    itemCallback: DiffUtil.ItemCallback<T> = defaultItemCallback(),
    detectMoves: Boolean = true
) {
    if (this.isEmpty()) {
        if (nextList.isNotEmpty()) {
            addAll(nextList)
        }
    } else {
        if (nextList.isEmpty()) {
            clear()
        } else {
            val stateList = this
            val diffResult = DiffUtil_Fixed3.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ) = itemCallback.areItemsTheSame(
                    stateList[oldItemPosition],
                    nextList[newItemPosition]
                )

                override fun getOldListSize() = stateList.size

                override fun getNewListSize() = nextList.size

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ) = itemCallback.areContentsTheSame(
                    stateList[oldItemPosition],
                    nextList[newItemPosition]
                )
            }, detectMoves)
            diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
                override fun onChanged(position: Int, count: Int, payload: Any?) {
                    for (i in position until (position + count)) {
                        stateList[i] = nextList[i]
                    }
                }

                override fun onMoved(fromPosition: Int, toPosition: Int) {
                    stateList.add(toPosition, stateList.removeAt(fromPosition))
                }

                override fun onInserted(position: Int, count: Int) {
                    stateList.addAll(position, nextList.subList(position, position + count))
                }

                override fun onRemoved(position: Int, count: Int) {
                    repeat(count) { stateList.removeAt(position) }
                }
            }.logChanges(target = stateList))
        }
    }
}

fun ListUpdateCallback.logChanges(
    target: List<*>? = null,
    logger: (String) -> Unit = { print(it) }
) = LoggingListUpdateCallback(target, this, logger)

class LoggingListUpdateCallback(
    private val stateList: List<*>? = null,
    private val delegate: ListUpdateCallback,
    private val logger: (String) -> Unit = { print(it) }
) : ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {
        logger("C(pos=$position, count=$count):".padEnd(25))
        val state = stateList?.toString()
        delegate.onChanged(position, count, payload)
        stateList?.let { logger("$state -> $stateList") }
        logger(System.lineSeparator())
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        logger("M(from=$fromPosition, to=$toPosition):".padEnd(25))
        val state = stateList?.toString()
        delegate.onMoved(fromPosition, toPosition)
        stateList?.let { logger("$state -> $stateList") }
        logger(System.lineSeparator())
    }

    override fun onInserted(position: Int, count: Int) {
        logger("I(pos=$position, count=$count):".padEnd(25))
        val state = stateList?.toString()
        delegate.onInserted(position, count)
        stateList?.let { logger("$state -> $stateList") }
        logger(System.lineSeparator())
    }

    override fun onRemoved(position: Int, count: Int) {
        logger("R(pos=$position, count=$count):".padEnd(25))
        val state = stateList?.toString()
        delegate.onRemoved(position, count)
        stateList?.let { logger("$state -> $stateList") }
        logger(System.lineSeparator())
    }
}


internal class ReportingListUpdateCallback(
    val stateList: MutableList<Any?> = ArrayList(),
    private val logger: (String) -> Unit = { print(it) }
) : ListUpdateCallback {

    private var diffTargetSourceProvider: (() -> List<Any?>)? = null
    private val targetSource: List<Any?>
        get() = diffTargetSourceProvider?.invoke() ?: emptyList()

    internal enum class OperationType {
        REMOVE, INSERT, MOVE, CHANGE;

        override fun toString(): String {
            return this.name.substring(0, 1)
        }
    }

    internal sealed class DiffOperation(val type: OperationType) {

        companion object {
            private val insertOperationsPool = Pools.SynchronizedPool<Insert>(50)
            private val removeOperationsPool = Pools.SynchronizedPool<Remove>(50)
            private val moveOperationsPool = Pools.SynchronizedPool<Move>(50)
            private val changeOperationsPool = Pools.SynchronizedPool<Change>(50)

            fun insert(position: Int, count: Int) = insertOperationsPool.acquire()
                ?.apply { this.position = position; this.count = count } ?: Insert(position, count)

            fun remove(position: Int, count: Int) = removeOperationsPool.acquire()
                ?.apply { this.position = position; this.count = count } ?: Remove(position, count)

            fun move(from: Int, to: Int) =
                moveOperationsPool.acquire()?.apply { this.from = from; this.to = to } ?: Move(
                    from,
                    to
                )

            fun change(position: Int, count: Int, payload: Any? = null) =
                changeOperationsPool.acquire()
                    ?.apply { this.position = position; this.count = count } ?: Change(
                    position,
                    count,
                    payload
                )
        }

        abstract fun recycle(): Boolean

        data class Insert internal constructor(var position: Int, var count: Int) :
            DiffOperation(OperationType.INSERT) {

            override fun recycle() = insertOperationsPool.release(this)

            override fun toString(): String = "I(position=$position, count=$count)"
        }

        data class Remove internal constructor(var position: Int, var count: Int) :
            DiffOperation(OperationType.REMOVE) {
            override fun recycle() = removeOperationsPool.release(this)

            override fun toString(): String = "R(position=$position, count=$count)"
        }

        data class Move internal constructor(var from: Int, var to: Int) :
            DiffOperation(OperationType.MOVE) {

            override fun recycle() = moveOperationsPool.release(this)

            override fun toString(): String = "M(from=$from, to=$to)"
        }

        data class Change internal constructor(
            var position: Int,
            var count: Int,
            var payload: Any? = null
        ) : DiffOperation(OperationType.CHANGE) {

            override fun recycle() = changeOperationsPool.release(this)

            override fun toString(): String =
                "C(position=$position, count=$count,${payload?.let { ", L: $it" } ?: ""})"
        }
    }

    private val insertRemoveOperations: Deque<DiffOperation> = LinkedList()
    private val moveChangeOperations: Deque<DiffOperation> = LinkedList()

    private fun enqueueOperation(operation: DiffOperation) {
        logger("$operation${System.lineSeparator()}")

        when (operation.type) {
            OperationType.REMOVE, OperationType.INSERT -> insertRemoveOperations
            OperationType.MOVE, OperationType.CHANGE -> moveChangeOperations
        }.push(operation)
    }

    fun executeOperations() {
        logger("Execute:${System.lineSeparator()}")

        var startOffset = 0
        while (insertRemoveOperations.isNotEmpty()) {
            executeOperation(insertRemoveOperations.pop()) { operation ->
                when (operation) {
                    is DiffOperation.Remove -> {
                        repeat(operation.count) { stateList.removeAt(operation.position + startOffset) }
                        startOffset -= operation.count
                    }
                    is DiffOperation.Insert -> {
                        val newElements = ArrayList<Any?>()
                        for (i in operation.position + startOffset until operation.position + startOffset + operation.count) {
                            newElements.add(targetSource[i])
                        }
                        stateList.addAll(operation.position + startOffset, newElements)
                        startOffset += operation.count
                    }
                    else -> throw IllegalStateException()
                }
                operation.recycle()
            }
        }

        while (moveChangeOperations.isNotEmpty()) {
            executeOperation(moveChangeOperations.removeLast()) { operation ->
                when (operation) {
                    is DiffOperation.Move -> {
                        stateList.add(
                            operation.to,
                            stateList.removeAt(operation.from)
                        )
                    }
                    else -> throw IllegalStateException()
                }
                operation.recycle()
            }
        }
    }

    fun setSource(source: (() -> List<Any?>)?) {
        if (diffTargetSourceProvider != source) {
            this.diffTargetSourceProvider = source
            this.stateList.clear()
            this.stateList.addAll(targetSource)
        }
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        enqueueOperation(change(position, count, payload))
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        enqueueOperation(move(fromPosition, toPosition))
    }

    override fun onInserted(position: Int, count: Int) {
        enqueueOperation(insert(position, count))
    }

    override fun onRemoved(position: Int, count: Int) {
        enqueueOperation(remove(position, count))
    }

    override fun toString(): String = stateList.toString()

    private inline fun executeOperation(
        op: DiffOperation,
        crossinline operation: (DiffOperation) -> Unit
    ) {
        logger("${op.toString().padEnd(25)} $this -> ")
        var error: Throwable? = null
        try {
            operation.invoke(op)
        } catch (error1: Exception) {
            error = error1
            throw error1
        } finally {
            if (error != null) {
                logger("(Error) ${error.javaClass.simpleName}${System.lineSeparator()}")
            } else {
                logger("$this${System.lineSeparator()}")
            }
        }
    }

    private fun checkSourceAttached() {
        check(diffTargetSourceProvider !== null) { "setSource() not called." }
    }
}