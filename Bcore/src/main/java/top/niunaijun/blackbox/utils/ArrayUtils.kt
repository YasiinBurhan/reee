package top.niunaijun.blackbox.utils

import java.util.Arrays
import java.util.Objects

object ArrayUtils {

    @JvmStatic
    fun <T> trimToSize(array: Array<T>?, size: Int): Array<T>? {
        if (array == null || size == 0) {
            return null
        } else if (array.size == size) {
            return array
        } else {
            return Arrays.copyOf(array, size)
        }
    }

    @JvmStatic
    fun push(array: Array<Any?>, item: Any?): Array<Any?> {
        val longer = arrayOfNulls<Any>(array.size + 1)
        System.arraycopy(array, 0, longer, 0, array.size)
        longer[array.size] = item
        return longer
    }

    @JvmStatic
    fun <T> contains(array: Array<T>?, value: T): Boolean {
        return indexOf(array, value) != -1
    }

    @JvmStatic
    fun contains(array: IntArray?, value: Int): Boolean {
        if (array == null) return false
        for (element in array) {
            if (element == value) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun <T> indexOf(array: Array<T>?, value: T): Int {
        if (array == null) return -1
        for (i in array.indices) {
            if (Objects.equals(array[i], value)) return i
        }
        return -1
    }

    @JvmStatic
    fun protoIndexOf(array: Array<Class<*>>?, type: Class<*>): Int {
        if (array == null) return -1
        for (i in array.indices) {
            if (array[i] == type) return i
        }
        return -1
    }

    @JvmStatic
    fun indexOfFirst(array: Array<Any?>?, type: Class<*>): Int {
        if (!isEmpty(array)) {
            var n = -1
            for (one in array!!) {
                n++
                if (one != null && type == one.javaClass) {
                    return n
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun protoIndexOf(array: Array<Class<*>>?, type: Class<*>, sequence: Int): Int {
        if (array == null) {
            return -1
        }
        var seq = sequence
        while (seq < array.size) {
            if (type == array[seq]) {
                return seq
            }
            seq++
        }
        return -1
    }

    @JvmStatic
    fun indexOfObject(array: Array<Any?>?, type: Class<*>, sequence: Int): Int {
        if (array == null) {
            return -1
        }
        var seq = sequence
        while (seq < array.size) {
            val item = array[seq]
            if (item != null && type.isInstance(item)) {
                return seq
            }
            seq++
        }
        return -1
    }

    @JvmStatic
    fun indexOf(array: Array<Any?>?, type: Class<*>, sequence: Int): Int {
        if (!isEmpty(array)) {
            var seq = sequence
            var n = -1
            for (one in array!!) {
                n++
                if (one != null && one.javaClass == type) {
                    if (--seq <= 0) {
                        return n
                    }
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun indexOfLast(array: Array<Any?>?, type: Class<*>): Int {
        if (!isEmpty(array)) {
            for (n in array!!.size downTo 1) {
                val one = array[n - 1]
                if (one != null && one.javaClass == type) {
                    return n - 1
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun <T> isEmpty(array: Array<T>?): Boolean {
        return array == null || array.isEmpty()
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T> getFirst(args: Array<Any?>, clazz: Class<*>): T? {
        val index = indexOfFirst(args, clazz)
        if (index != -1) {
            return args[index] as T?
        }
        return null
    }

    @JvmStatic
    @Throws(ArrayIndexOutOfBoundsException::class)
    fun checkOffsetAndCount(arrayLength: Int, offset: Int, count: Int) {
        if (offset or count < 0 || offset > arrayLength || arrayLength - offset < count) {
            throw ArrayIndexOutOfBoundsException(offset)
        }
    }
}
