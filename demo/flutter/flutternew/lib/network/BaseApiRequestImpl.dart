typedef OnSuccessCallback<T> = void Function(T response);
typedef OnThrowableCallback = void Function(Object error, StackTrace stackTrace);

class BaseApiRequestImpl {
  Future<void> sendRequestCallback<T>({
    required Future<T> Function() apiCall,
    OnSuccessCallback<T>? successCallback,
    OnThrowableCallback? throwableCallback,
  }) async {
    try {
      final response = await apiCall();
      successCallback?.call(response);
    } catch (error, stackTrace) {
      throwableCallback?.call(error, stackTrace);
    }
  }
}
