import '../../domain/dto/base_response.dart';
import '../../network/remote_api_exception.dart';

/// 对齐 Android [RemoteApiSource.requestData]：统一解包 [BaseResponse]。
Future<T> requestData<T>(
  Future<BaseResponse<T>> Function() apiCall, {
  String emptyDataMessage = '响应数据为空',
}) async {
  final response = await apiCall();
  if (!response.isSuccess) {
    throw RemoteApiException(response.code, response.message ?? '请求失败');
  }
  if (response.data == null) {
    throw RemoteApiException(response.code, emptyDataMessage);
  }
  return response.data as T;
}
