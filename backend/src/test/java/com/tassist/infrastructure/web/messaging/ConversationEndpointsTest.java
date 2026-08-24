package com.tassist.infrastructure.web.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.EmbeddingClient;
import com.tassist.domain.port.out.LLMClient;
import com.tassist.infrastructure.persistence.AbstractPgvectorContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** §9 messaging endpoints over real Postgres, using the shared singleton container. */
@AutoConfigureMockMvc
@Import(ConversationEndpointsTest.FakeAiConfig.class)
class ConversationEndpointsTest extends AbstractPgvectorContainerTest {

    @TestConfiguration
    static class FakeAiConfig {
        @Bean @Primary EmbeddingClient fakeEmbed() {
            return new EmbeddingClient() {
                public float[] embed(String t) { float[] v = new float[1024]; v[0] = 1f; return v; }
                public List<float[]> embedBatch(List<String> ts) {
                    List<float[]> o = new ArrayList<>(); for (String t : ts) o.add(embed(t)); return o; }
                public int dimension() { return 1024; }
            };
        }
        @Bean @Primary LLMClient fakeLlm() {
            return new LLMClient() {
                public LlmResponse complete(LlmRequest r) { return new LlmResponse("Grounded reply.", 10, 5); }
                public void stream(LlmRequest r, StreamEvents e) {}
            };
        }
    }

    @DynamicPropertySource
    static void jwt(DynamicPropertyRegistry r) {
        r.add("tassist.jwt.secret", () -> "test_secret_at_least_32_bytes_long_hs256_ok");
        r.add("tassist.storage.dir", () -> System.getProperty("java.io.tmpdir") + "/tassist-msg-test");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token(String email) throws Exception {
        String resp = mvc.perform(post("/api/auth/signup").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"displayName\":\"U\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }
    private String meId(String token) throws Exception {
        String resp = mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }
    private String createChannel(String token, String username) throws Exception {
        String resp = mvc.perform(post("/api/channels").header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"username\":\"" + username + "\",\"displayName\":\"Chan\",\"visibility\":\"PUBLIC\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }
    /** member requests join (returns membershipId), owner approves by that id. */
    private void joinAndApprove(String channelId, String memberToken, String ownerToken) throws Exception {
        String joinResp = mvc.perform(post("/api/channels/" + channelId + "/join")
                .header("Authorization", "Bearer " + memberToken)
                .contentType("application/json").content("{\"message\":\"pls\"}"))
            .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        String membershipId = json.readTree(joinResp).get("id").asText();
        mvc.perform(post("/api/channels/" + channelId + "/members/" + membershipId + "/approve")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void dm_openPostList_flow_andAccessControl() throws Exception {
        String owner = token("owner-a@t.dev"), alice = token("alice-a@t.dev"), bob = token("bob-a@t.dev");
        String chan = createChannel(owner, "chan-msg-a");
        joinAndApprove(chan, alice, owner);
        joinAndApprove(chan, bob, owner);
        String bobId = meId(bob);

        String dmResp = mvc.perform(post("/api/channels/" + chan + "/dm").header("Authorization", "Bearer " + alice)
                .contentType("application/json").content("{\"targetUserId\":\"" + bobId + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.kind").value("DM"))
            .andReturn().getResponse().getContentAsString();
        String convId = json.readTree(dmResp).get("id").asText();

        mvc.perform(post("/api/channels/" + chan + "/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + alice)
                .contentType("application/json").content("{\"content\":\"hey bob\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message.content").value("hey bob"));

        mvc.perform(get("/api/channels/" + chan + "/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + bob))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].content").value("hey bob"));

        // owner (not a participant) forbidden from this member↔member DM
        mvc.perform(get("/api/channels/" + chan + "/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + owner))
            .andExpect(status().isForbidden());
    }

    @Test
    void openDm_isIdempotent() throws Exception {
        String owner = token("owner-b@t.dev"), alice = token("alice-b@t.dev");
        String chan = createChannel(owner, "chan-msg-b");
        joinAndApprove(chan, alice, owner);
        String ownerId = meId(owner), aliceId = meId(alice);

        String r1 = mvc.perform(post("/api/channels/" + chan + "/dm").header("Authorization", "Bearer " + alice)
                .contentType("application/json").content("{\"targetUserId\":\"" + ownerId + "\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(post("/api/channels/" + chan + "/dm").header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"targetUserId\":\"" + aliceId + "\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json.readTree(r1).get("id").asText())
            .isEqualTo(json.readTree(r2).get("id").asText());
    }

    @Test
    void participants_includesOwner() throws Exception {
        String owner = token("owner-c@t.dev"), alice = token("alice-c@t.dev");
        String chan = createChannel(owner, "chan-msg-c");
        joinAndApprove(chan, alice, owner);
        mvc.perform(get("/api/channels/" + chan + "/participants").header("Authorization", "Bearer " + alice))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.isOwner==true)]").exists());
    }

    @Test
    void outsider_cannotOpenDm() throws Exception {
        String owner = token("owner-d@t.dev"), alice = token("alice-d@t.dev"), outsider = token("outsider-d@t.dev");
        String chan = createChannel(owner, "chan-msg-d");
        joinAndApprove(chan, alice, owner);
        String aliceId = meId(alice);
        mvc.perform(post("/api/channels/" + chan + "/dm").header("Authorization", "Bearer " + outsider)
                .contentType("application/json").content("{\"targetUserId\":\"" + aliceId + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void groupToggle_ownerOnly_disablingBlocksMembers() throws Exception {
        String owner = token("owner-e@t.dev"), alice = token("alice-e@t.dev");
        String chan = createChannel(owner, "chan-msg-e");
        joinAndApprove(chan, alice, owner);

        mvc.perform(put("/api/channels/" + chan + "/group/enabled").header("Authorization", "Bearer " + alice)
                .contentType("application/json").content("{\"enabled\":false}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/channels/" + chan + "/group/enabled").header("Authorization", "Bearer " + owner)
                .contentType("application/json").content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/channels/" + chan + "/group").header("Authorization", "Bearer " + alice))
            .andExpect(status().isForbidden());
    }
}
