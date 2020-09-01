package com.example.diffutilbug

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil_4
import androidx.recyclerview.widget.ListUpdateCallback
import org.junit.Assert
import org.junit.Test

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
        testDiffForItems("ABC", "CBDA")
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_11() {
        testDiffForItems('B'..'D', ('A'..'D').reversed())
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_12() {
        testDiffForItems('A'..'C', ('A'..'C').reversed().plus('E'..'F'))
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

    @Test
    fun item_changes_are_correct_for_lists_with_duplicate_items_1() {
        testDiffForItems("ABBCCD", "CDCBAB")
    }

    @Test
    fun item_changes_are_correct_for_lists_with_duplicate_items_2() {
        testDiffForItems("AAXBB", "AXAXBXB")
    }

    @Test
    fun item_changes_are_correct_for_lists_with_duplicate_items_3() {
        testDiffForItems(mutableListOf<Char?>().apply {
            this += 'A'
            this.add(null)
            this += 'B'
            this.add(null)
            this.add(null)
            this += 'B'
            this += 'A'
        }, mutableListOf<Char?>().apply {
            this += 'A'
            this += 'A'
            this.add(null)
            this.add(null)
            this.add(null)
            this += 'B'
            this += 'B'
        })
    }

    @Test
    fun test_Item_Content_Changes_1() {
        testDiffForItems("ABCD", "abcd")
    }

    @Test
    fun test_Item_Content_Changes_2() {
        testDiffForItems("ABCD", "aBcD")
    }

    @Test
    fun test_Item_Content_Changes_With_Moved_Items_1() {
        testDiffForItems("ABCD", "bcda")
    }

    @Test
    fun test_Item_Content_Changes_With_Moved_Items_2() {
        testDiffForItems("abcd", "BCDA")
    }

    @Test
    fun test_Item_Content_Changes_With_Moved_Items_3() {
        testDiffForItems("aBCd", "bcDA")
    }

    private fun testDiffForItems(vararg lists: CharSequence) =
        this.testDiffForItems(*lists.map { it.toList() }.toTypedArray())

    private fun testDiffForItems(vararg lists: Iterable<Char?>) {
        val state = mutableListOf<Char?>()
        lists.forEach {
            diffList(state, it.toList())
        }
    }


    private fun diffList(currentList: MutableList<Char?>, nextList: List<Char?>) {
        logger("\nScript without move detection:\n")
        logger("Diff: $currentList -> ${nextList}\n")
        val stateCopy = currentList.toMutableList()
        stateCopy.patchTo(nextList, detectMoves = false, itemCallback = CHAR_DIFF_ITEM_CALLBACK)
        assertConsistency(stateCopy, nextList)

        logger("\nScript with move detection:\n\n")
        logger("\nDiff: $currentList -> ${nextList}\n")
        currentList.patchTo(nextList, detectMoves = true, itemCallback = CHAR_DIFF_ITEM_CALLBACK)
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

private val CHAR_DIFF_ITEM_CALLBACK = object : DiffUtil.ItemCallback<Char?>() {
    override fun areItemsTheSame(oldItem: Char, newItem: Char): Boolean {
        return oldItem?.toLowerCase() == newItem?.toLowerCase()
    }

    override fun areContentsTheSame(oldItem: Char, newItem: Char): Boolean {
        return oldItem == newItem
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
    val stateList = this
    val oldListSize = stateList.size
    val nextListSize = nextList.size
    val diffResult = DiffUtil_4.calculateDiff(object : DiffUtil.Callback() {
        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            val oldItem = stateList[oldItemPosition]
            val newItem = nextList[newItemPosition]
            return (oldItem != null && newItem != null) && itemCallback.areItemsTheSame(
                oldItem,
                newItem
            )
        }

        override fun getOldListSize() = oldListSize

        override fun getNewListSize() = nextListSize

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

            var oldIndex = position
            val endIndex = position + count
            while (oldIndex < endIndex) {
                val newIndex = diffResult.convertOldPositionToNew(oldIndex)
                stateList[oldIndex] = nextList[newIndex]
                oldIndex++
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            stateList.add(toPosition, stateList.removeAt(fromPosition))
        }

        override fun onInserted(position: Int, count: Int) {
            val startPos = if (position > 0) {
                diffResult.convertOldPositionToNew(position - 1) + 1
            } else {
                0
            }
            stateList.addAll(position, nextList.subList(startPos, startPos + count))
        }

        override fun onRemoved(position: Int, count: Int) {
            repeat(count) { stateList.removeAt(position) }
        }
    }.logChanges(target = stateList))
}

fun emptyCallback() = object : ListUpdateCallback {
    override fun onChanged(position: Int, count: Int, payload: Any?) {
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
    }

    override fun onInserted(position: Int, count: Int) {
    }

    override fun onRemoved(position: Int, count: Int) {
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
