import '../domain/dto/BaseResponse.dart';
import '../domain/dto/req/UserTestReq.dart';
import '../domain/dto/resp/UserTestResp.dart';

import 'package:retrofit/retrofit.dart';
import 'package:dio/dio.dart';
import '../constant/NetworkConstant.dart';

// 核心：声明生成的 .g.dart 文件（Retrofit 和 JSON 序列化）
part 'ApiRequest.g.dart';

// Retrofit 核心注解：标记这是 API 接口类
@RestApi(baseUrl: NetworkConstant.baseUrl)
abstract class ApiRequest {
  // 工厂方法：创建 ApiRequest 实例（由 retrofit_generator 生成实现）
  factory ApiRequest(Dio dio, {String baseUrl}) = _ApiRequest;

  // POST 请求：注册接口
  @POST('/test/network/register')
  Future<BaseResponse<UserTestResp>> register(@Body() UserTestReq req);

  // GET 请求：重置 Token 接口
  @GET('/test/network/resetToken')
  Future<BaseResponse<UserTestResp>> resetToken(@Query('account') String account);
}
