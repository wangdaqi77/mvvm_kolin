package com.wongki.framework.mvvm.lifecycle

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.wongki.framework.mvvm.action.EventAction
import com.wongki.framework.mvvm.AbsLiveDataViewModel
import java.lang.RuntimeException
import kotlin.reflect.KClass

/**
 * @author  wangqi
 * date:    2019/7/4
 * email:   wangqi7676@163.com
 * desc:    .
 *          ViewModel接口
 *          核心：管理多个子LiveData，管理多种类型数据变动
 */
interface ILiveDataViewModel {

    val mSystemLiveData: HashMap<String, MutableLiveData<DataWrapper<*>>?>


    private fun <T : Any> getKey(type: DataType, clazz: KClass<T>) = "${type.name}<${clazz.java.name}>"

    /**
     * 生成子live data
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> fork(clazz: KClass<T>): MutableLiveData<DataWrapper<T>> {
        val key = getKey(DataType.Normal, clazz)
        if (!mSystemLiveData.containsKey(key)) {
            mSystemLiveData[key] = MutableLiveData<DataWrapper<T>>() as MutableLiveData<DataWrapper<*>>
        }
        return mSystemLiveData[key] as MutableLiveData<DataWrapper<T>>
    }

    /**
     * 生成子live data
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> forkForArrayList(clazz: KClass<T>): MutableLiveData<DataWrapper<ArrayList<T>>> {
        val key = getKey(DataType.ArrayList, clazz)
        if (!mSystemLiveData.containsKey(key)) {
            mSystemLiveData[key] = MutableLiveData<DataWrapper<ArrayList<T>>>() as MutableLiveData<DataWrapper<*>>
        }
        return mSystemLiveData[key] as MutableLiveData<DataWrapper<ArrayList<T>>>
    }

    /**
     * 获取子live data
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getLiveData(clazz: KClass<T>): MutableLiveData<DataWrapper<T>> {
        val key = getKey(DataType.Normal, clazz)
        if (!mSystemLiveData.containsKey(key)) {
            throw RuntimeException("call this before please call fork")
        }
        return mSystemLiveData[key] as MutableLiveData<DataWrapper<T>>
    }

    /**
     * 获取子live data
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getLiveDataForArrayList(clazz: KClass<T>): MutableLiveData<DataWrapper<ArrayList<T>>> {
        val key = getKey(DataType.ArrayList, clazz)
        if (!mSystemLiveData.containsKey(key)) {
            throw RuntimeException("call this before please call fork")
        }
        return mSystemLiveData[key] as MutableLiveData<DataWrapper<ArrayList<T>>>
    }



    fun <T : Any> setValue(responseType: KClass<T>, action: EventAction, apply: DataWrapper<T>.() -> Unit):DataWrapper<T> {
        val dataWrapper = DataWrapper<T>()
        dataWrapper.action = action
        apply(dataWrapper)
        // 发送到订阅的位置
        getLiveData(responseType).setValue(dataWrapper)
        return dataWrapper
    }


    /**
     * 发送数据到接收者[observe]
     */
    fun <T : Any> KClass<T>.setValueForAction(action: EventAction) {
        setValue(this, action) {}
    }


    /**
     * 发送数据到接收者[observe]
     */
    fun <T : Any> setValueForArrayList(responseType: KClass<T>, action: EventAction, apply: DataWrapper<ArrayList<T>>.() -> Unit):DataWrapper<ArrayList<T>> {
        val dataWrapper = DataWrapper<ArrayList<T>>()
        dataWrapper.action = action
        apply(dataWrapper)
        // 发送到订阅的位置
        getLiveDataForArrayList(responseType).setValue(dataWrapper)
        return dataWrapper
    }


    /**
     * 发送数据到接收者[observe]
     */
    fun <T : Any> KClass<T>.setValueForArrayListForAction(action: EventAction) {
        setValueForArrayList(this, action) {}
    }



//
//
//    /**
//     * 发送数据到接收者[observe]
//     */
//    @Deprecated("过时")
//    fun <T : Any> postValue(responseType: KClass<T>, action: EventAction, apply: DataWrapper<T>.() -> Unit):DataWrapper<T> {
//        val dataWrapper = DataWrapper<T>()
//        dataWrapper.action = action
//        apply(dataWrapper)
//        // 发送到订阅的位置
//        getLiveData(responseType).postValue(dataWrapper)
//        return dataWrapper
//    }
//
//    /**
//     * 发送数据到接收者[observe]
//     */
//    fun <T : Any> KClass<T>.postValueForAction(action: EventAction) {
//        postValue(this, action) {}
//    }
//
//    /**
//     * 发送数据到接收者[observe]
//     */
//    @Deprecated("过时")
//    fun <T : Any> postValueForArrayList(responseType: KClass<T>, action: EventAction, apply: DataWrapper<ArrayList<T>>.() -> Unit):DataWrapper<ArrayList<T>> {
//        val dataWrapper = DataWrapper<ArrayList<T>>()
//        dataWrapper.action = action
//        apply(dataWrapper)
//        // 发送到订阅的位置
//        getLiveDataForArrayList(responseType).postValue(dataWrapper)
//        return dataWrapper
//    }
//
//
//    /**
//     * 发送数据到接收者[observe]
//     */
//    fun <T : Any> KClass<T>.postValueForArrayListForAction(action: EventAction) {
//        postValueForArrayList(this, action) {}
//    }

}


/**
 * 订阅，接收数据变化的事件通知
 * 通知数据变化->[AbsLiveDataViewModel.commit]
 * @param onFailed 返回true代表上层处理，返回false代表框架处理，目前框架层会弹Toast
 */
fun <T : Any> MutableLiveData<DataWrapper<T>>?.observeSimple(
    owner: LifecycleOwner,
    onFailed: (Int, String?) -> Boolean = { _, _ -> false },
    onSuccess: (T?) -> Unit
) {
    if (this == null) {
        Log.e("LiveDataViewModel", "MutableLiveData is null")
        return
    }

    this.observe(owner, Observer<DataWrapper<T>> { result ->
        if (result != null) {
            val action = result.action
            when (action) {
                EventAction.FAILED -> {
                    val errorProcessed = onFailed(result.code, result.message)
                    result.errorProcessed = errorProcessed
                }

                EventAction.SUCCESS -> {
                    onSuccess(result.data)
                }
                else -> {
                    Log.e("LiveDataViewModel", "未处理的Action：${action.name}")
                }
            }
        }

    })
}


/**
 * 订阅，接收数据变化的事件通知
 * 通知数据变化->[AbsLiveDataViewModel.commit]
 * @param onFailed 返回true代表上层处理，返回false代表框架处理，目前框架层会弹Toast
 */
fun <T : Any> MutableLiveData<DataWrapper<T>>?.observe(
    owner: LifecycleOwner,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onFailed: (Int, String?) -> Boolean,
    onSuccess: (T?) -> Unit
) {
    if (this == null) {
        Log.e("LiveDataViewModel", "MutableLiveData is null")
        return
    }

    this.observe(owner, Observer<DataWrapper<T>> { result ->
        if (result != null) {
            val action = result.action
            when (action) {
                EventAction.START -> {
                    onStart()
                }
                EventAction.CANCEL -> {
                    onCancel()
                }
                EventAction.SUCCESS -> {
                    onSuccess(result.data)
                }
                EventAction.FAILED -> {
                    val errorProcessed = onFailed(result.code, result.message)
                    result.errorProcessed = errorProcessed
                }
                else -> {
                    Log.e("LiveDataViewModel", "未处理的Action：${action.name}")
                }
            }
        }

    })
}