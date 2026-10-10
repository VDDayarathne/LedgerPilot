package com.ledgerpilot.ledgerpilot_backend;

import com.jayway.jsonpath.JsonPath;
import com.ledgerpilot.ledgerpilot_backend.auth.application.DevMailbox;
import com.ledgerpilot.ledgerpilot_backend.auth.application.TokenCodec;
import com.ledgerpilot.ledgerpilot_backend.auth.domain.OneTimeToken;
import com.ledgerpilot.ledgerpilot_backend.auth.persistence.OneTimeTokenRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.util.*;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PhaseOneJourneyIntegrationTests {
    @Autowired MockMvc mvc; @Autowired DevMailbox mailbox; @Autowired OneTimeTokenRepository oneTimeTokens; @Autowired TokenCodec tokenCodec;

    @Test void browserPreflightAllowsConfiguredFrontendOrigin() throws Exception {
        mvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    @Test void registrationRefreshTenantInvitationAndIsolationJourney() throws Exception {
        String suffix=UUID.randomUUID().toString().substring(0,8), owner="owner-"+suffix+"@example.test", employee="employee-"+suffix+"@example.test";
        registerAndVerify(owner,"Owner Person"); Login ownerLogin=login(owner);

        MvcResult created=mvc.perform(post("/api/v1/organizations").header("Authorization","Bearer "+ownerLogin.access()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Isolation Test Ltd\"}"))
                .andExpect(status().isCreated()).andReturn();
        String tenantId=JsonPath.read(created.getResponse().getContentAsString(),"$.id");
        String ownerTenantToken=select(ownerLogin.access(),tenantId);

        MvcResult rotated=mvc.perform(post("/api/v1/auth/refresh").cookie(ownerLogin.refresh(),ownerLogin.xsrf()).header("X-CSRF-TOKEN",ownerLogin.xsrf().getValue()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString()).andReturn();
        Cookie rotatedRefresh=rotated.getResponse().getCookie("refresh_token"),rotatedXsrf=rotated.getResponse().getCookie("XSRF-TOKEN");
        mvc.perform(post("/api/v1/auth/refresh").cookie(ownerLogin.refresh(),ownerLogin.xsrf()).header("X-CSRF-TOKEN",ownerLogin.xsrf().getValue()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefresh,rotatedXsrf).header("X-CSRF-TOKEN",rotatedXsrf.getValue()))
                .andExpect(status().isUnauthorized());

        registerAndVerify(employee,"Employee Person"); Login employeeLogin=login(employee);
        mvc.perform(post("/api/v1/invitations").header("Authorization","Bearer "+ownerTenantToken).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+employee+"\",\"role\":\"EMPLOYEE\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/invitations").header("Authorization","Bearer "+ownerTenantToken).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"escalation@example.test\",\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OWNER_TRANSFER_REQUIRED"));
        String invite=latest(employee,"INVITATION");
        mvc.perform(post("/api/v1/invitations/accept").header("Authorization","Bearer "+employeeLogin.access()).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+invite+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(tenantId));
        mvc.perform(post("/api/v1/invitations/accept").header("Authorization","Bearer "+employeeLogin.access()).contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+invite+"\"}"))
                .andExpect(status().isBadRequest());
        String employeeTenantToken=select(employeeLogin.access(),tenantId);
        mvc.perform(post("/api/v1/organizations/"+UUID.randomUUID()+"/select").header("Authorization","Bearer "+employeeLogin.access()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/organization").header("Authorization","Bearer "+employeeTenantToken).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Escalated\",\"countryCode\":\"LK\",\"fiscalYearStartMonth\":1}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
        MvcResult members=mvc.perform(get("/api/v1/members").header("Authorization","Bearer "+ownerTenantToken)).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2)).andReturn();
        List<Map<String,Object>> content=JsonPath.read(members.getResponse().getContentAsString(),"$.content");
        String ownerMember=content.stream().filter(m->"OWNER".equals(m.get("role"))).findFirst().orElseThrow().get("id").toString();
        String employeeMember=content.stream().filter(m->"EMPLOYEE".equals(m.get("role"))).findFirst().orElseThrow().get("id").toString();
        mvc.perform(patch("/api/v1/members/"+ownerMember+"/role").header("Authorization","Bearer "+ownerTenantToken).contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("OWNER_TRANSFER_REQUIRED"));
        mvc.perform(patch("/api/v1/members/"+employeeMember+"/status").header("Authorization","Bearer "+ownerTenantToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/organization").header("Authorization","Bearer "+employeeTenantToken)).andExpect(status().isForbidden());
    }

    @Test void passwordResetTokensExpireAndAreSingleUse() throws Exception {
        String email="recovery-"+UUID.randomUUID().toString().substring(0,8)+"@example.test";
        registerAndVerify(email,"Recovery Person");
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\"}"))
                .andExpect(status().isOk());
        String expired=latest(email,"PASSWORD_RESET");
        OneTimeToken record=oneTimeTokens.findAll().stream().filter(t->t.getTokenHash().equals(tokenCodec.hash(expired))&&t.getType()==OneTimeToken.Type.PASSWORD_RESET).findFirst().orElseThrow();
        record.setExpiresAt(Instant.now().minusSeconds(1));oneTimeTokens.saveAndFlush(record);
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+expired+"\",\"password\":\"A different strong password!\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\"}"))
                .andExpect(status().isOk());
        String valid=latest(email,"PASSWORD_RESET");
        String resetBody="{\"token\":\""+valid+"\",\"password\":\"A different strong password!\"}";
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(resetBody)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(resetBody)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"A different strong password!\"}" )).andExpect(status().isOk());
    }

    private void registerAndVerify(String email,String name) throws Exception {mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"displayName\":\""+name+"\",\"password\":\"Correct horse battery staple!\"}" )).andExpect(status().isCreated());String token=latest(email,"VERIFICATION");mvc.perform(post("/api/v1/auth/verify-email").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\""+token+"\"}")).andExpect(status().isOk());}
    private Login login(String email) throws Exception {MvcResult r=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"Correct horse battery staple!\"}" )).andExpect(status().isOk()).andReturn();String access=JsonPath.read(r.getResponse().getContentAsString(),"$.accessToken");return new Login(access,r.getResponse().getCookie("refresh_token"),r.getResponse().getCookie("XSRF-TOKEN"));}
    private String select(String access,String tenant) throws Exception {MvcResult r=mvc.perform(post("/api/v1/organizations/"+tenant+"/select").header("Authorization","Bearer "+access)).andExpect(status().isOk()).andReturn();return JsonPath.read(r.getResponse().getContentAsString(),"$.accessToken");}
    private String latest(String email,String type){return mailbox.forEmail(email).stream().filter(m->m.type().equals(type)).reduce((a,b)->b).orElseThrow().token();}
    private record Login(String access,Cookie refresh,Cookie xsrf){}
}
