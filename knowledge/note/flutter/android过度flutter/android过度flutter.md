**Android2Flutter**
====




### 异步网络流程

参考Android的流程：
OkHttp + Retrofit + Kotlin携程 进行响应式异步http、ws请求
Gson进行数据序列化
ViewModel + LiveData在Activity上进行UI更新


Flutter对应流程

| Android 端技术            | Flutter 端技术                       | 核心作用            |
|------------------------|-----------------------------------|-----------------|
| OkHttp3                | Dio（主流）/dart:io（底层）               | HTTP/WSS 请求底层实现 |
| Retrofit2              | retrofit（基于 Dio 封装）               | 注解式接口封装、参数解析    |
| Kotlin 协程（Coroutine）   | Dart 异步（async/await）+ Isolate（可选） | 响应式异步请求         |
| Gson                   | json_serializable（编译期生成）          | JSON 序列化 / 反序列化 |
| ViewModel + LiveData   | Riverpod/Provider/Bloc/GetX       | 跨组件状态管理、UI 更新   |
| Activity/Fragment      | Flutter Widget（StatefulWidget）    | UI 渲染、生命周期管理    |


##### 创建网络Client + 序列化
网络请求首先要创建网络Client和序列化方法：
Android使用的Okhttp3创建网络Client
```kotlin
        // 创建 API 请求
fun <T> createApiRequest(
    apiClass: Class<T>,
    mainUrl: String,
    connectTimeOut: Long,
    readTimeOut: Long,
    writeTimeOut: Long,
    callTimeOut: Long,
    interceptors: List<Interceptor>
): T {
    val uploadOkHttpClient = createUploadOkHttpClient(
        connectTimeOut,
        readTimeOut,
        writeTimeOut,
        callTimeOut,
        interceptors
    )

    return Retrofit.Builder()
        .baseUrl(mainUrl)
        // Gson 转换器 进行序列化
        .addConverterFactory(GsonConverterFactory.create())
        .client(uploadOkHttpClient)
        .build()
        .create(apiClass)
}

// 创建 OkHttpClient
private fun createUploadOkHttpClient(
    connectTimeOut: Long,
    readTimeOut: Long,
    writeTimeOut: Long,
    callTimeOut: Long,
    interceptors: List<Interceptor>
): OkHttpClient {
    // 创建缓存目录
    val cacheFile = getCacheDir()
    val cache = Cache(cacheFile, 1024 * 1024 * 50) // 50MB 缓存大小

    // 创建日志拦截器实例
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY // 记录请求和响应的完整内容
        } else {
            HttpLoggingInterceptor.Level.NONE // 不记录任何日志
        }
    }

    // 创建 OkHttpClient.Builder
    val builder = OkHttpClient.Builder()
        .retryOnConnectionFailure(false) // 不重复请求
        .connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
        .writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS)
        .callTimeout(callTimeOut, TimeUnit.MILLISECONDS)
        .cache(cache)
        .addInterceptor(loggingInterceptor) // OkHttp3 日志拦截器
        .proxy(Proxy.NO_PROXY)

    // 添加传入的拦截器
    interceptors.forEach { builder.addInterceptor(it) }

    return builder.build()
}
```
Android 主要是使用`OkHttpClient.Builder()`创建`GsonConverterFactory`序列化。

Flutter的本身是禁用反射的，所以无法像Android一样直接使用Retrofit和Gson进行数据解析，
需要预编译。预编译的指令是
```shell
flutter packages pub run build_runner build
```
会生成`.g.dart`文件。

不使用预编译则手动序列化：
```dart
class UserTestReq {
  final String account;
  final String password;
  final String name;

  const UserTestReq({
    required this.account,
    required this.password,
    required this.name,
  });

  Map<String, dynamic> toJson() {
    return {
      'account': account,
      'password': password,
      'name': name,
    };
  }
}

BaseResponse<UserTestResp> _parseUserResponse(Response<dynamic> response) {
  final raw = _normalizeMap(response.data);
  return BaseResponse.fromJson(raw, (json) => UserTestResp.fromJson(json));
}
```

Flutter的网络请求不使用Okhttp，使用Dio
```dart
  ApiRequestImpl({Dio? dio})
      : _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: NetworkConstant.baseUrl,
                connectTimeout: 10000,
                receiveTimeout: 10000,
              ),
            );

  final Dio _dio;
```

##### 接口创建

然后创建API接口：
Android使用Retrofit自动注入创建接口：
```kotlin
interface ApiRequest {
    @GET("/agent/getInfo")
    suspend fun getAgentInfo(
        @Query("agentId") agentId: String
    ): BaseResponse<AgentResponse>
}
```
Retrofit创建的接口能自动的将接口地址和字段名称绑定
不适用预编译则接口：
```dart
// 定义接口
abstract class ApiRequest {
  Future<BaseResponse<UserTestResp>> register(UserTestReq req);
  Future<BaseResponse<UserTestResp>> resetToken(String account);
}

// 继承接口，写入url和出入参
@override
Future<BaseResponse<UserTestResp>> register(UserTestReq req) async {
  final response = await _dio.post(
    '/test/network/register',
    data: req.toJson(),
  );
  return _parseUserResponse(response);
}

@override
Future<BaseResponse<UserTestResp>> resetToken(String account) async {
  final response = await _dio.get(
    '/test/network/resetToken',
    queryParameters: {'account': account},
  );
  return _parseUserResponse(response);
}
```

接下来需要定义全部接口的执行流程：
Android跟Flutter的方式基本一致，都是定义接口和基类。
```kotlin
    fun getAgentInfo(
        agentId: String,
        onSuccessCallback: OnSuccessCallback<BaseResponse<AgentResponse>>?,
        throwableCallback: OnThrowableCallback?
    ){
        sendRequestCallback(
            apiCall = {
                mApi.getAgentInfo(agentId)
            },
            successCallback = onSuccessCallback,
            throwableCallback = throwableCallback
        )
    }
```
```dart
  Future<void> registerWithCallback(
    UserTestReq req,
    OnSuccessCallback<BaseResponse<UserTestResp>>? successCallback,
    OnThrowableCallback? throwableCallback,
  ) {
    return sendRequestCallback(
      apiCall: () => register(req),
      successCallback: successCallback,
      throwableCallback: throwableCallback,
    );
  }

  Future<void> resetTokenWithCallback(
    String account,
    OnSuccessCallback<BaseResponse<UserTestResp>>? successCallback,
    OnThrowableCallback? throwableCallback,
  ) {
    return sendRequestCallback(
      apiCall: () => resetToken(account),
      successCallback: successCallback,
      throwableCallback: throwableCallback,
    );
  }
```


##### 接口调用

Android一般使用ViewModel + LiveData + （ViewBinding + DataBinding / Jetpack Compose声明式）实现


```kotlin
    // 查询Agent
fun doGetAgentInfo(context: Context, agentId: String, callback: SyncRequestCallback){
    api.getAgentInfo(
        agentId,
        object : OnSuccessCallback<BaseResponse<AgentResponse>> {
            override fun onResponse(response: BaseResponse<AgentResponse>?) {
                AppResponseUtil.handleSyncResponseEx(
                    response,
                    context,
                    callback,
                    ::handleGetAgentInfo
                )
            }
        },
        object : OnThrowableCallback {
            override fun callback(throwable: Throwable?) {
                callback.onThrowable(throwable)
            }
        }
    )
}

private fun handleGetAgentInfo(response: BaseResponse<AgentResponse>?,
                               context: Context,
                               callback: SyncRequestCallback) {
    response?.data?.agentAo?.let { ao ->

        ao.agentVo?.let { vo ->
            aao.avatarUrlLd.postValue(vo.avatarUrl)
            aao.nameLd.postValue(vo.name)
            aao.descriptionLd.postValue(vo.description)
        }
    }
    callback.onAllRequestSuccess()
}
```

```dart
  Future<void> login({
    required String account,
    required String password,
  }) async {
    if (_isLoading) return;
    _setLoading(true);

    final req = UserTestReq(
      account: account,
      password: password,
      name: account,
    );

    await _api.register(
      req,
      _handleLoginSuccess,
      _handleThrowable,
    );

    _setLoading(false);
  }


void _handleLoginSuccess(BaseResponse<UserTestResp> response) {
  if (response.isSuccess && response.data != null) {
    _account = response.data?.account ?? '';
    _loginToken = response.data?.loginToken ?? '';
    _isLoggedIn = true;
    _statusColor = Colors.green;
    _statusMessage = _buildSuccessMessage('登录成功');
  } else {
    _setErrorMessage(response.message ?? '登录失败');
  }
  notifyListeners();
}
```

在Android中更新数据使用的是`postValue()`, 在Flutter中更新数据使用`notifyListeners();`

##### 数据绑定

任务例子：获取AgentAI的信息，获取过程中需要显示加载进度条。

Android使用XML + （ViewBinding + DataBinding / Jetpack Compose声明式）

###### Android XML实现

xml终究会被淘汰，只是目前是主流，因为UI写在xml中无法动态修改（函数入参），还需要把view写在xml中。

首先定义xml：
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical">

    <!-- 加载进度条（binding.progressBar） -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"/>

</LinearLayout>
```


Android中Activity的声明周期切换或者结束会导致数据丢失，所以需要用声明周期更长的viewModel来存储数据。
```kotlin
class LoadingViewModel : ViewModel() {
    // 对应你示例中的isLoadingLd
    val isLoadingLd = MutableLiveData<Boolean>(false)

    // 对应你示例的handleResult
    fun handleResult(response: BaseResponse<AgentResponse>?) {
        isLoadingLd.postValue(false)
    }

    // 开始加载
    fun startLoading() {
        isLoadingLd.postValue(true)
    }
}
```

在activity中调用数据加载，注册viewModel并观察数据变化，然后编写数据绑定逻辑。
```kotlin
class MainActivity : AppCompatActivity() {
    // ViewBinding：替代findViewById
    private lateinit var binding: ActivityMainBinding
    private lateinit var vm: LoadingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 用ViewModelProvider初始化ViewModel
        vm = ViewModelProvider(this)[LoadingViewModel::class.java]

        // 观察加载状态并绑定到ProgressBar
        observeData()

        // binding点击监听
        binding.root.setOnClickListener {
            vm.startLoading()
            // 模拟网络请求后调用handleResult
            kotlinx.coroutines.GlobalScope.launch {
                kotlinx.coroutines.delay(2000)
                vm.handleResult(BaseResponse(200, AgentResponse("1", "测试"), "success"))
            }
        }
    }

    // 观察数据变化
    private fun observeData() {
        // 观察livedata
        vm.isLoadingLd.observe(this) { isLoading ->
            // 绑定状态到ProgressBar的可见性
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}
```


###### Android Jetpack Compose实现

Jetpack Compose是声明式UI

Compose 会「自动监听」viewModel.isLoading（StateFlow）的变化，一旦 isLoading 的值从 true 变成 false（或反过来），
Compose 会自动重新执行当前可组合函数，并更新 UI

Activity和UI
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 根布局
            LoadingScreen()
        }
    }
}


@Composable
fun LoadingScreen(
    viewModel: LoadingViewModel = viewModel() // 自动获取ViewModel
) {
    // 将StateFlow转换为Compose可感知的状态（自动监听变化）
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 加载控件：CircularProgressIndicator（对应XML的ProgressBar）
        // 实时的，只要发生变化就改变显示状态，所以不用写false的时候隐藏逻辑
        if (isLoading) {
            CircularProgressIndicator()
        }

        // 模拟触发加载（点击屏幕开始加载）
        androidx.compose.foundation.clickable.ClickableText(
            text = androidx.compose.ui.text.AnnotatedString("点击开始加载"),
            onClick = {
                viewModel.startLoading()
                // 模拟2秒后结束加载
                runBlocking {
                    launch {
                        delay(2000)
                        viewModel.handleResult(BaseResponse(200, AgentResponse("1", "测试"), "success"))
                    }
                }
            }
        )
    }
}
```
定义viewModel：
```kotlin
class LoadingViewModel : ViewModel() {
    // Compose推荐用StateFlow替代LiveData（响应式状态）
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 对应handleResult：结束加载
    fun handleResult(response: BaseResponse<AgentResponse>?) {
        _isLoading.value = false
    }

    // 开始加载
    fun startLoading() {
        _isLoading.value = true
    }
}
```
StateFlow 与 LiveData类似

###### Flutter 实现

Flutter 实现（ChangeNotifier + Consumer）


定义 ViewModel（ChangeNotifier 替代 ViewModel+LiveData）
```dart
import 'package:flutter/foundation.dart';

// Flutter的ViewModel：继承ChangeNotifier管理状态
class LoadingViewModel extends ChangeNotifier {
  // 加载状态（对应isLoadingLd）
  bool _isLoading = false;
  bool get isLoading => _isLoading;

  // 对应handleResult：结束加载
  void handleResult(BaseResponse<AgentResponse>? response) {
    _isLoading = false;
    notifyListeners(); // 通知UI更新（对应postValue）
  }

  // 开始加载
  void startLoading() {
    _isLoading = true;
    notifyListeners(); // 通知UI更新
  }
}
```


Flutter 页面（Consumer 绑定状态）
```dart
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

void main() {
  runApp(
    // 全局提供ViewModel（对标Android的ViewModelProvider）
    ChangeNotifierProvider(
      create: (context) => LoadingViewModel(),
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        body: LoadingPage(),
      ),
    );
  }
}

class LoadingPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Consumer<LoadingViewModel>(
      // 监听isLoading变化，仅重建该组件（细粒度更新）
      builder: (context, viewModel, child) {
        return GestureDetector(
          // 点击屏幕触发加载
          onTap: () {
            viewModel.startLoading();
            // 模拟2秒后结束加载
            Future.delayed(const Duration(seconds: 2), () {
              viewModel.handleResult(
                BaseResponse(200, AgentResponse("1", "测试"), "success"),
              );
            });
          },
          child: Center(
            child: viewModel.isLoading
                ? Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: const [
                      // Flutter的加载控件（对应Android的ProgressBar）
                      CircularProgressIndicator(),
                      SizedBox(height: 16),
                      Text("加载中...", style: TextStyle(fontSize: 16)),
                    ],
                  )
                : const Text("点击屏幕开始加载"),
          ),
        );
      },
    );
  }
}
```
`Consumer<LoadingViewModel>` 类似 `LiveData.observe()` 回调，监听 ChangeNotifier 的状态变化






