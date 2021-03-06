package code.api

import code.api.util.ErrorMessages
import code.bankconnectors.vJune2017.InboundAccountJune2017
import code.bankconnectors.vMar2017.InboundStatusMessage
import code.setup.{APIResponse, DefaultUsers, ServerSetup}
import net.liftweb.common.Full
import net.liftweb.json
import net.liftweb.json.Extraction
import net.liftweb.json.JsonAST.{JField, JObject, JString}
import net.liftweb.util.Props
import org.scalatest._
import code.api.util.APIUtil.OAuth._
import code.api.util.ApiRole.CanGetAnyUser
import code.api.util.ErrorMessages.UserHasMissingRoles

class gateWayloginTest extends ServerSetup with BeforeAndAfter with DefaultUsers {

  //fake this: Connector.connector.vend.getBankAccounts(username)
  val fakeResultFromAdapter =  Full(InboundAccountJune2017(
    errorCode = "",
    List(InboundStatusMessage("ESB", "Success", "0", "OK")),
    cbsToken ="cbsToken1",
    bankId = "gh.29.uk",
    branchId = "222",
    accountId = "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0",
    accountNumber = "123",
    accountType = "AC",
    balanceAmount = "50",
    balanceCurrency = "EUR",
    owners = "Susan" :: " Frank" :: Nil,
    viewsToGenerate = "Public" :: "Accountant" :: "Auditor" :: Nil,
    bankRoutingScheme = "iban",
    bankRoutingAddress = "bankRoutingAddress",
    branchRoutingScheme = "branchRoutingScheme",
    branchRoutingAddress = " branchRoutingAddress",
    accountRoutingScheme = "accountRoutingScheme",
    accountRoutingAddress = "accountRoutingAddress"
  ) :: InboundAccountJune2017(
    errorCode = "",
    List(InboundStatusMessage("ESB", "Success", "0", "OK")),
    cbsToken ="cbsToken2",
    bankId = "gh.29.uk",
    branchId = "222",
    accountId = "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0",
    accountNumber = "123",
    accountType = "AC",
    balanceAmount = "50",
    balanceCurrency = "EUR",
    owners = "Susan" :: " Frank" :: Nil,
    viewsToGenerate = "Public" :: "Accountant" :: "Auditor" :: Nil,
    bankRoutingScheme = "iban",
    bankRoutingAddress = "bankRoutingAddress",
    branchRoutingScheme = "branchRoutingScheme",
    branchRoutingAddress = " branchRoutingAddress",
    accountRoutingScheme = "accountRoutingScheme",
    accountRoutingAddress = "accountRoutingAddress"
  ) ::Nil)


  val accessControlOriginHeader = ("Access-Control-Allow-Origin", "*")
  /* Payload data. verified by secret "0844b5b8-4f27-488b-9eb6-6db2327a838b"
    {
      "login_user_name":"simonr",
      "is_first":false,
      "app_id":"593450734587345",
      "app_name":"myapp4",
      "time_stamp":"19-06-2017:22:27:11:100",
      "cbs_token":"",
      "temenos_id":""
    }
    */
  val invalidSecretJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2dpbl91c2VyX25hbWUiOiJzaW1vbnIiLCJpc19maXJzdCI6dHJ1ZSwiYXBwX2lkIjoiNTkzNDUwNzM0NTg3MzQ1IiwiYXBwX25hbWUiOiJteWFwcDQiLCJ0aW1lX3N0YW1wIjoiMTktMDYtMjAxNzoyMjoyNzoxMToxMDAiLCJjYnNfdG9rZW4iOiIiLCJ0ZW1lbm9zX2lkIjoiIn0.XwpHG0XupGlOfIlPrYGgM2duJQNH_sxrkKqnhtIVxLU"
  /* Payload data. verified by secret "Cannot get the secret"
  {
    "login_user_name":"simonr",
    "is_first":false,
    "app_id":"593450734587345",
    "app_name":"myapp4",
    "time_stamp":"19-06-2017:22:27:11:100",
    "cbs_token":"",
    "temenos_id":""
  }
  */
  val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsb2dpbl91c2VyX25hbWUiOiJzaW1vbnIiLCJpc19maXJzdCI6ZmFsc2UsImFwcF9pZCI6IjU5MzQ1MDczNDU4NzM0NSIsImFwcF9uYW1lIjoibXlhcHA0IiwidGltZV9zdGFtcCI6IjE5LTA2LTIwMTc6MjI6Mjc6MTE6MTAwIiwiY2JzX3Rva2VuIjoiIiwidGVtZW5vc19pZCI6IiJ9.SH0SXU_IQ0jD6i2HexyKoV7DBMm8Ox1_ADXt-WQjJJw"

  val invalidJwt = ("Authorization", ("GatewayLogin token=%s").format(invalidSecretJwt))
  val validJwt = ("Authorization", ("GatewayLogin token=%s").format(jwt))
  val missingParameterToken = ("Authorization", ("GatewayLogin wrong_parameter_name=%s").format(jwt))

  def gatewayLoginRequest = baseRequest / "obp" / "v3.0.0" / "users"
  def gatewayLoginNonBlockingRequest = baseRequest / "obp" / "v3.0.0" / "users" / "current" / "customers"

  feature("GatewayLogin in a BLOCKING way") {
    Props.getBool("allow_gateway_login", false) match  {
      case true =>
        scenario("Missing parameter token in a blocking way") {
          When("We try to login without parameter token in a Header")
          val request = gatewayLoginRequest
          val response = makeGetRequest(request, List(missingParameterToken))
          Then("We should get a 400 - Bad Request")
          response.code should equal(400)
          assertResponse(response, ErrorMessages.GatewayLoginMissingParameters + "token")
        }

        scenario("Invalid JWT value") {
          When("We try to login with an invalid JWT")
          val request = gatewayLoginRequest
          val response = makeGetRequest(request, List(invalidJwt))
          Then("We should get a 400 - Bad Request")
          response.code should equal(400)
          assertResponse(response, ErrorMessages.GatewayLoginJwtTokenIsNotValid)
        }

        scenario("Valid JWT value") {
          When("We try to login with an valid JWT")
          val request = gatewayLoginRequest.GET <@ (userGatewayLogin)
          val response = makeGetRequest(request, List(validJwt))
            println("-----------------------------------------")
            println(response)
            println("-----------------------------------------")
          Then("We should get a 400 - Bad Request because we miss a proper role")
          response.code should equal(400)
          assertResponse(response, UserHasMissingRoles + CanGetAnyUser)
        }
      case false =>
        logger.info("-----------------------------------------------------------------------------------")
        logger.info("------------- GatewayLogin in a BLOCKING way Test is DISABLED ---------------------")
        logger.info("-----------------------------------------------------------------------------------")
    }
  }

  feature("GatewayLogin in a NON BLOCKING way") {
    Props.getBool("allow_gateway_login", false) match  {
      case true =>
        scenario("Missing parameter token in a blocking way") {
          When("We try to login without parameter token in a Header")
          val request = gatewayLoginNonBlockingRequest
          val response = makeGetRequest(request, List(missingParameterToken))
          Then("We should get a 400 - Bad Request")
          response.code should equal(400)
          assertResponse(response, ErrorMessages.GatewayLoginMissingParameters + "token")
        }

        scenario("Invalid JWT value") {
          When("We try to login with an invalid JWT")
          val request = gatewayLoginNonBlockingRequest
          val response = makeGetRequest(request, List(invalidJwt))
          Then("We should get a 400 - Bad Request")
          response.code should equal(400)
          assertResponse(response, ErrorMessages.GatewayLoginJwtTokenIsNotValid)
        }

        scenario("Valid JWT value") {
          When("We try to login with an valid JWT")
          val request = gatewayLoginNonBlockingRequest.GET <@ (userGatewayLogin)
          val response = makeGetRequest(request, List(validJwt))
          Then("We should get a 200 ")
          response.code should equal(200)
        }
      case false =>
        logger.info("---------------------------------------------------------------------------------------")
        logger.info("------------- GatewayLogin in a NON BLOCKING way Test is DISABLED ---------------------")
        logger.info("---------------------------------------------------------------------------------------")
    }
  }


  feature("Unit Tests for two getCbsToken and getErrors: ") {
    scenario("test the getErrors") {
      val reply: List[String] =  GatewayLogin.getErrors(json.compactRender(Extraction.decompose(fakeResultFromAdapter.openOrThrowException("Attempted to open an empty Box."))))
      reply.forall(_.equalsIgnoreCase("")) should equal(true)
    }

    scenario("test the getCbsToken") {
      val reply: List[String] =  GatewayLogin.getCbsTokens(json.compactRender(Extraction.decompose(fakeResultFromAdapter.openOrThrowException("Attempted to open an empty Box."))))
      reply(0) should equal("cbsToken1")
      reply(1) should equal("cbsToken2")

      reply.exists(_.equalsIgnoreCase("")==false) should equal(true)
    }
  }



  private def assertResponse(response: APIResponse, expectedErrorMessage: String): Unit = {
    response.body match {
      case JObject(List(JField(name, JString(value)))) =>
        name should equal("error")
        value should include(expectedErrorMessage)
      case _ => fail("Expected an error message")
    }
  }
}