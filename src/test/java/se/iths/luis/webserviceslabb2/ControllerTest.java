package se.iths.luis.webserviceslabb2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.CoreMatchers.*;

@WebMvcTest(Controller.class)
@Import({MailModelAssembler.class})
public class ControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MailRepository mockRepository;

    @BeforeEach
    void setup(){
        when(mockRepository.findAll()).thenReturn(List.of(new Mail(1L,"test@test.com","Test","Test1"),new Mail(2L,"test@test.com","Test","Test2")));
        when(mockRepository.findById(1L)).thenReturn(Optional.of(new Mail(1L,"test@test.com","Test","Test1")));
        when(mockRepository.save(any(Mail.class))).thenAnswer(invocationOnMock -> {
           Object[] args = invocationOnMock.getArguments();
           var m = (Mail) args[0];
           return new Mail(1L,m.getTo(),m.getSubject(),m.getText());
        });
    }

    @Test
    void getAllMails() throws Exception {
        mockMvc.perform(
                get("/api/mails").contentType("application/hal+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.mailList[0]._links.self.href", is("http://localhost/api/mails/1")))
                .andExpect(jsonPath("_embedded.mailList[0].to", is("test@test.com")));
        //Build json paths with: https://jsonpath.com/
    }

    @Test
    @DisplayName("Calls Get method with url /api/mails/1")
    void getMailbyID() throws Exception {
        mockMvc.perform(
                get("/api/mails/1").accept("application/hal+json"))
                .andExpect(status().isOk())
                //.andExpect(jsonPath("content[0].links[2].rel", is("self")))
                .andExpect(jsonPath("_links.self.href", is("http://localhost/api/mails/1")));
    }

    @Test
    @DisplayName("Calls Get method with invalid id url /api/mails/3")
    void getInvalidID() throws Exception {
        mockMvc.perform(
                get("/api/mails/3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCreateMail() throws Exception {
        mockMvc.perform(
                post("/api/mails/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":0,\"to\":\"test@test.com\",\"subject\":\"Test\",\"text\":\"Test1\"}"))
                .andExpect(status().isCreated());
    }


}

