package com.example.diffutilbug

import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.*
import java.util.concurrent.Executor

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ListUpdateCallbackTest {

    private lateinit var asyncListDiffer: AsyncListDiffer<Char>
    private lateinit var listUpdateCallback: ReportingListUpdateCallback
    private val charsDifCallback = object : DiffUtil.ItemCallback<Char>() {
        override fun areItemsTheSame(oldItem: Char, newItem: Char): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Char, newItem: Char): Boolean {
            return oldItem == newItem
        }
    }

    private val logger: (String) -> Unit = { print(it) }

    @Before
    fun setUp() {
        listUpdateCallback = ReportingListUpdateCallback(logger)

        val immediateExecutor = Executor { command -> command.run() }
        val asyncDifferConfig = AsyncDifferConfig.Builder(charsDifCallback)
            .setMainThreadExecutor(immediateExecutor)
            .setBackgroundThreadExecutor(immediateExecutor)
            .build()
        asyncListDiffer = AsyncListDiffer(listUpdateCallback, asyncDifferConfig)
        listUpdateCallback.setSource { asyncListDiffer.currentList }
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_1() {
        asyncListDiffer.submitLists('C'..'E', 'A'..'H')
        listUpdateCallback.assertConsistency()
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_2() {
        asyncListDiffer.submitLists('A'..'D', 'A'..'H')
        listUpdateCallback.assertConsistency()
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_3() {
        asyncListDiffer.submitLists('A'..'D', 'A'..'H')
        listUpdateCallback.assertConsistency()
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_4() {
        asyncListDiffer.submitLists('A'..'D', ('A'..'D').reversed())
        listUpdateCallback.assertConsistency()
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_5() {
        asyncListDiffer.submitLists('A'..'H', 'C'..'E')
        listUpdateCallback.assertConsistency()
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_6() {
        asyncListDiffer.submitLists('B'..'C', 'A'..'D')
        listUpdateCallback.assertConsistency()
    }

    @Test
    fun listUpdateCallback_Changes_Are_Reported_In_Order_7() {
        asyncListDiffer.submitLists("CDEFIJKL", "ABCDEFGHIKLMN")
        listUpdateCallback.assertConsistency()
    }

    private fun AsyncListDiffer<Char>.submitLists(vararg lists: CharSequence) =
        this.submitLists(*lists.map { it.toList() }.toTypedArray())

    private fun <T> AsyncListDiffer<T>.submitLists(vararg lists: Iterable<T>) {
        lists.forEach {
            val nextList = it.toList()
            logger("\nSubmit list:\n Old: ${asyncListDiffer.currentList}\n New: ${nextList}\n\n")
            this.submitList(nextList)
        }
    }
}

/**
 * An [ListUpdateCallback] implementation that applies and logs any changes
 * coming from calls to the callback with a given external list.
 * */
internal class ReportingListUpdateCallback(
    private val logger: (String) -> Unit = { print(it) }
) : ListUpdateCallback {

    private var diffTargetSourceProvider: (() -> List<*>)? = null
    private val stateList: MutableList<Any?> = ArrayList()
    private val targetSource: List<Any?>
        get() = diffTargetSourceProvider?.invoke() ?: emptyList()

    val mutatedSource: List<Any?> =
        Collections.unmodifiableList(stateList)

    fun setSource(source: (() -> List<*>)?) {
        if (diffTargetSourceProvider != source) {
            this.diffTargetSourceProvider = source
            this.stateList.clear()
            this.stateList.addAll(targetSource)
        }
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        checkSourceAttached()
        executeOperation("Change(index=$position, count=$count${payload?.let { ", L: $it" }
            ?: ""})") {
            for (i in position until position + count) {
                stateList[i] = targetSource[i]
            }
        }
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        checkSourceAttached()
        executeOperation("Move(from=$fromPosition, to=$toPosition)") {
            stateList.add(toPosition, stateList.removeAt(fromPosition))
        }
    }

    override fun onInserted(position: Int, count: Int) {
        checkSourceAttached()
        executeOperation("Insert(index=$position, count=$count)") {
            val newItems = ArrayList<Any?>()
            for (i in position until position + count) {
                newItems.add(targetSource[i])
            }

            stateList.addAll(position, newItems)
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        checkSourceAttached()
        executeOperation("Remove(index=$position, count=$count)") {
            for (i in position + count - 1 downTo position) {
                stateList.removeAt(i)
            }
        }
    }

    fun assertConsistency() {
        val expectedList = targetSource
        val actualList = this.stateList

        if (expectedList.size != actualList.size) {
            Assert.assertEquals(
                "Item count mismatch, expected (${expectedList.size}), but was (${actualList.size}).",
                expectedList, stateList
            )
        }
        for (position in expectedList.indices) {
            val expectedItem = expectedList[position]
            val actualItem = actualList[position]
            if (actualItem != expectedItem) {
                Assert.assertEquals(
                    "Inconsistency at ($position), expected `$expectedItem`, but was '$actualItem'.",
                    expectedList, actualList
                )
            }
        }
    }

    override fun toString(): String = stateList.toString()

    private inline fun executeOperation(name: String, crossinline operation: () -> Unit) {
        logger("${name.padEnd(13)}:")
        var error: Throwable? = null
        try {
            operation.invoke()
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