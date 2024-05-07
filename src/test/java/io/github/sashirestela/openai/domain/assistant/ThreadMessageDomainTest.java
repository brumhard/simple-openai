package io.github.sashirestela.openai.domain.assistant;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.DomainTestingHelper;
import io.github.sashirestela.openai.domain.assistant.Attachment.AttachmentTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

public class ThreadMessageDomainTest {

    static HttpClient httpClient;
    static SimpleOpenAI openAI;

    @BeforeAll
    static void setup() {
        httpClient = mock(HttpClient.class);
        openAI = SimpleOpenAI.builder()
                .apiKey("apiKey")
                .httpClient(httpClient)
                .build();
    }

    @Test
    void testCreateThreadMessage() throws IOException {
        DomainTestingHelper.get().mockForObject(httpClient, "src/test/resources/threads_messages_create.json");
        var threadMessageRequest = ThreadMessageRequest.builder()
                .role(ThreadMessageRole.USER)
                .content("Please, tell me what an LLM is?")
                .attachment(Attachment.builder()
                        .fileId("fileId")
                        .tool(AttachmentTool.FILE_SEARCH)
                        .build())
                .metadata(Map.of("item", "first"))
                .build();
        var threadMessage = openAI.threadMessages().create("threadId", threadMessageRequest).join();
        System.out.println(threadMessage);
        assertNotNull(threadMessage);
    }

    @Test
    void testModifyThreadMessage() throws IOException {
        DomainTestingHelper.get().mockForObject(httpClient, "src/test/resources/threads_messages_modify.json");
        var threadMessageModifyRequest = ThreadMessageModifyRequest.builder()
                .metadata(Map.of("item", "firstly", "user", "dummy"))
                .build();
        var threadMessage = openAI.threadMessages()
                .modify("threadId", "threadMessageId", threadMessageModifyRequest)
                .join();
        System.out.println(threadMessage);
        assertNotNull(threadMessage);
    }

    @Test
    void testRetrieveThreadMessage() throws IOException {
        DomainTestingHelper.get().mockForObject(httpClient, "src/test/resources/threads_messages_getone.json");
        var threadMessage = openAI.threadMessages().getOne("threadId", "threadMessageId").join();
        System.out.println(threadMessage);
        assertNotNull(threadMessage);
    }

    @Test
    void testListThreadMessage() throws IOException {
        DomainTestingHelper.get().mockForObject(httpClient, "src/test/resources/threads_messages_getlist.json");
        var threadMessages = openAI.threadMessages().getList("threadId").join();
        threadMessages.forEach(System.out::println);
        assertNotNull(threadMessages);
    }

    @Test
    void testDeleteThreadMessage() throws IOException {
        DomainTestingHelper.get().mockForObject(httpClient, "src/test/resources/threads_messages_delete.json");
        var deletedThreadMessage = openAI.threadMessages().delete("threadId", "threadMessageId").join();
        System.out.println(deletedThreadMessage);
        assertNotNull(deletedThreadMessage);
    }

}
