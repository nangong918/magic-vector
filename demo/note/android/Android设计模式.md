**Android设计模式**
====




### MVC




### MVP




### MVVM

* MVC和MVP的UI需要手动去获取R.id非常的麻烦，所以使用viewBinding
* MVC和MVP的UI数据互通每次都要更新数据之后再手动去设置给UI，
  MVVM中用用Livedata + dataBinding直接数据互通绑定到XML。
* MVC和MVP中的UI生命周期难以管理，比如说屏幕旋转，UI数据会丢失，
  MVVM中使用ViewModel去管理数据。ViewModel中的数据存活比Activity更长。



### MVI


MVI中

Intent用户意图，用户的行为通过intent交给viewModel处理。
Effect副作用流，用户意图带的副作用流交给Activity处理。
State状态：UI数据状态，相当于LiveData。在Compose中只要设置了State状态UI就会自动改变，相当于MVVM设计模式中的dataBinding。
















