**Android生命周期**
====



#### Activity的生命周期
Activity生命周期
1. onCreate()：创建的时候
    * 绑定View：binding = Binding.inflate(layoutInflater)
    * 注册ActivityResultLauncher
    * 注册viewModel，注册观察者。
    * 初始化CameraX（但是不绑定生命周期）
    * Service绑定
    * Button监听器绑定
2. onStart()：不可见，准备前台可见
    * 广播接收器
    * EventBus注册
    * 初始化网络请求（可能需要Service绑定后的资源，一般在Service绑定成功的回调中执行。）
    * CameraX绑定生命周期
3. onResume()：与用户交互阶段
    * 开启CameraX
    * 开启YOLOv8检测
    * 开启AudioRecord
    * 开启AudioTrack
    * 动画绘制
4. onPause()：暂停交互 (不要在此处解绑生命周期和释放资源，因为系统可能只是短暂失去焦点（如弹出对话框）)
    * 暂停音频/视频播放
    * 提交未保存的更改（例如草稿）
5. onStop()：清理可见相关资源
    * 停止YOLOv8检测
    * 停止AudioRecord
    * 停止AudioTrack
    * CameraX解绑生命周期
6. onDestroy()：最终清理
    * 解绑Service
    * 释放资源


Activity的行为导致生命周期的变化：
* 横屏反转、横竖屏反转
   ```text
   原Activity: onPause() -> onStop() -> onDestroy()
   新Activity: onCreate() -> onStart() -> onResume()
   ```
* Activity1 startActivity(intent) 跳转 Activity2 -> Activity2 finish销毁回到 Activity1
   ```text
   Activity1: onPause() -> onStop()
   Activity2: onCreate() -> onStart() -> onResume()
   --- 用户在Activity2中按返回或调用finish() ---
   Activity2: onPause()
   Activity1: onRestart() -> onStart() -> onResume()
   Activity2: onStop() -> onDestroy()
   ```
* Activity1 ActivityResultLauncher(intent) 跳转 Activity2 -> Activity2 finish销毁回到 Activity1
    * 流程同上，但是在Activity2中调用setResult()后调用finish()。Activity1在onResume()之后，会通过ActivityResultLauncher的回调接收到结果
* Android Home键回到桌面 -> 从桌面返回App时的生命周期
   ```text
   按下Home键: onPause() -> onStop()
   从桌面返回: onRestart() -> onStart() -> onResume()
   ```

#### Fragment的生命周期

（其实在Compose中就不需要Fragment的，组合函数本身就是Fragment，生命周期兼容）

Fragment生命周期类似Activity：



```text
onAttach() → onCreate() → onCreateView() → onViewCreated() → onStart() → onResume()
→ onPause() → onStop() → onDestroyView() → onDestroy() → onDetach()
```
不同点：
`onAttach()`: Fragment与Activity关联时调用
* 此时的Fragment的 `isAdded()` 方法是true
  `onCreateView()`: 创建Fragment的UI布局
* 进行binding填充
  `onViewCreated()`: View已创建完成，最适合初始化UI相关操作
* 初始化View数据, liveData观察数据
  `onDestroyView()`: View被销毁，但Fragment实例仍然存在

关于`isAdded()`：在`onAttach()`之后是true，在 `onDetach()` 之后是false。

关于什么时候能获取到Fragment的ViewModel？
* 场景1：获取Fragment自己的ViewModel
```kotlin
// 在任何生命周期方法中都可以，但推荐在onCreate或onViewCreated中
class MyFragment : Fragment() {
    // ✅ 方式1：在onCreate中获取（最早的位置）
    private lateinit var viewModel: MyViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
    }
    
    // ✅ 方式2：使用property delegate（推荐）
    private val viewModel: MyViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 这里可以安全使用viewModel
        viewModel.data.observe(viewLifecycleOwner) { data ->
            binding.textView.text = data
        }
    }
}
```
* 场景2：获取共享的Activity ViewModel（Fragment间通信）
```kotlin
class MyFragment : Fragment() {
    // 获取所在Activity的ViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.sharedData.observe(viewLifecycleOwner) { data ->
            // 多个Fragment可以观察同一个数据
        }
    }
}
```
Fragment自己的ViewModel: 从onCreate()开始就可以获取

Activity的共享ViewModel: 从onAttach()之后就可以获取

#### ViewModel的生命周期
ViewModel的生命周期比Activity长，能在Activity不是真正销毁的时候保留数据。
Activity的 finish() 和 屏幕旋转 都会导致 onDestroy() ，但是两者的销毁步兵不同。
* 屏幕旋转：清理viewModel的viewModelScope
* finish()：清理viewModel的viewModelScope

创建ViewModel：
由于viewModel的生命周期比Activity长，所以创建的时候需要借助ViewModelProvider进行获取和管理viewModel。首次获取则创建，非首次则获取。
```kotlin
class MyActivity : AppCompatActivity() {
    // 最简单的方式
    private val viewModel: MyViewModel by viewModels()
    
    // 带Factory的方式
    private val viewModelWithFactory: MyViewModel by viewModels { 
        MyViewModelFactory("参数") 
    }
}

class MyActivity : AppCompatActivity() {
   private lateinit var viewModel: MyViewModel

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // 传统方式
      viewModel = ViewModelProvider(this).get(MyViewModel::class.java)

      // 带Factory的传统方式
      val factory = MyViewModelFactory("参数")
      viewModel = ViewModelProvider(this, factory).get(MyViewModel::class.java)
   }
}
```
销毁viewModel：
系统调用onDestroy()之后，viewModel的onCleared()会被调用。
```kotlin
class MyViewModel : ViewModel() {
    private val networkRequest: Job? = null
    private val databaseConnection: Closeable? = null
    
    override fun onCleared() {
        super.onCleared()
        // 清理持有的资源
        networkRequest?.cancel()
        databaseConnection?.close()
        Log.d("ViewModel", "清理所有资源")
    }
}
```

#### Service和Application的生命周期
Service的生命周期：
* 创建：onCreate()
* 启动：onStart()
* 停止：onStop()
* 销毁：onDestroy()
* 绑定：onBind()
* 解绑：onUnbind()

Application的生命周期：
* 创建：onCreate()
* 销毁：onTerminate()

Application可以放置任何全局变量。
Service应该放置跨Activity任务。




























