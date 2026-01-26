package devtechly.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import devtechly.service.MailService;
import devtechly.service.dto.ContactRequestDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@org.junit.jupiter.api.Disabled("Mockito initialization issue on this environment")
@WebMvcTest(controllers = ContactResource.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MailService mailService;

    @Test
    void sendContactEmail_returnsOk_whenMailSent() throws Exception {
        ContactRequestDTO dto = new ContactRequestDTO();
        dto.setPriority("info");
        dto.setSubject("Sujet valide");
        dto.setMessage("Bonjour, ceci est un message de test suffisamment long.");
        dto.setTimestamp(java.time.Instant.now().toString());

        mockMvc
            .perform(post("/api/contact/send-email").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Votre message a été envoyé avec succès"));

        Mockito.verify(mailService).sendEmail(any(String.class), any(String.class), any(String.class), anyBoolean(), anyBoolean());
    }

    @Test
    void sendContactEmail_returnsBadRequest_whenMailFails() throws Exception {
        ContactRequestDTO dto = new ContactRequestDTO();
        dto.setPriority("high");
        dto.setSubject("Sujet valide");
        dto.setMessage("Bonjour, ce message va provoquer une erreur d'envoi.");
        dto.setTimestamp(java.time.Instant.now().toString());

        doThrow(new RuntimeException("smtp down"))
            .when(mailService)
            .sendEmail(any(String.class), any(String.class), any(String.class), anyBoolean(), anyBoolean());

        mockMvc
            .perform(post("/api/contact/send-email").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }
}
