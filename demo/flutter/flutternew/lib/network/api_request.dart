import 'package:dio/dio.dart';
import 'package:retrofit/retrofit.dart';

import '../constant/network_constant.dart';
import '../domain/dto/base_response.dart';
import '../domain/dto/req/user_test_req.dart';
import '../domain/dto/resp/user_test_resp.dart';

part 'api_request.g.dart';

@RestApi(baseUrl: NetworkConstant.baseUrl)
abstract class ApiRequest {
  factory ApiRequest(Dio dio, {String baseUrl}) = _ApiRequest;

  @POST('/test/network/register')
  Future<BaseResponse<UserTestResp>> register(@Body() UserTestReq req);

  @GET('/test/network/resetToken')
  Future<BaseResponse<UserTestResp>> resetToken(@Query('account') String account);
}
