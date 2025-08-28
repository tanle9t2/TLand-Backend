import grpc
from generated import post_pb2_grpc, post_pb2
from google.protobuf.json_format import MessageToDict, MessageToJson
import asyncio
from service.embeeding_service import index_to_pinecone, feed_db


async def run():
    # Connect to the server (Spring Boot gRPC server, e.g. running on localhost:9090)
    channel = grpc.insecure_channel("localhost:9194")
    stub = post_pb2_grpc.PostToSearchServiceStub(channel)

    # Build request
    request = post_pb2.Empty(
    )

    # Call service
    response = stub.getAllPost(request)

    response_dict = MessageToDict(response, preserving_proto_field_name=True)

    await feed_db(response_dict)


if __name__ == "__main__":
    asyncio.run(run())
