package dynoxide.scripted

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model._

import java.net.URI
import scala.jdk.CollectionConverters._

/**
 * Roundtrip test proving the Dynoxide emulator, started by `DynoxidePlugin` before this test
 * runs, is a real, working DynamoDB-compatible endpoint — not just an HTTP port responding to the
 * plugin's readiness probe.
 */
class DynoxideRoundtripSuite extends munit.FunSuite {

  private val TableName = "scripted-simple-table"

  private val client: DynamoDbClient = DynamoDbClient
    .builder()
    .endpointOverride(URI.create("http://localhost:18000"))
    .region(Region.US_EAST_1)
    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
    .build()

  override def afterAll(): Unit = client.close()

  test("PutItem/GetItem roundtrip against the Dynoxide emulator") {
    client.createTable(
      CreateTableRequest
        .builder()
        .tableName(TableName)
        .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
        .attributeDefinitions(
          AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
        )
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .build()
    )

    client.putItem(
      PutItemRequest
        .builder()
        .tableName(TableName)
        .item(Map("id" -> AttributeValue.fromS("item-1"), "value" -> AttributeValue.fromS("hello-dynoxide")).asJava)
        .build()
    )

    val response = client.getItem(
      GetItemRequest
        .builder()
        .tableName(TableName)
        .key(Map("id" -> AttributeValue.fromS("item-1")).asJava)
        .build()
    )

    assert(response.hasItem)
    assertEquals(response.item().get("value").s(), "hello-dynoxide")
  }
}
