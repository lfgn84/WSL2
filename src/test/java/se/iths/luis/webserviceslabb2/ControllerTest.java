package se.iths.luis.webserviceslabb2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.CoreMatchers.is;

@WebMvcTest(Controller.class)
@Import({MailModelAssembler.class})
public class ControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MailRepository repository;

    @MockBean
    JavaMailSender mailSender;

    @BeforeEach
    void setup(){

        when(repository.findAll()).thenReturn(List.of(new Mail(1L, "lfgn84@gmail.com","Test 1","Test Mail 1",null), new Mail(2L, "seventythree73@hotmail.com","Test 2","Test Mail 2",null), new Mail(3L,"lfgn84@gmail.com","Test 3","Test mail 3",null)));
        when(repository.findById(1L)).thenReturn(Optional.of(new Mail(1L, "lfgn84@gmail.com","Test 1","Test Mail 1",null)));
        when(repository.findById(2L)).thenReturn(Optional.of(new Mail(2L, "seventythree73@hotmail.com","Test 2","Test Mail 2",null)));
        when(repository.existsById(3L)).thenReturn(true);
        when(repository.findById(3L)).thenReturn( Optional.of(new Mail(3L,"lfgn84@gmail.com","Test 3","Test mail 3",null)));
        when(repository.save(any(Mail.class))).thenAnswer(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            var m = (Mail) args[0];
            return new Mail(0L, m.getTo(),m.getSubject(),m.getText(),m.getSent());
        });

    }

    @Test
    void getAllMails() throws Exception {
        mockMvc.perform(
                get("/api/mails").contentType("application/hal+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.mailList[0]._links.self.href", is("http://localhost/api/mails/1")))
                .andExpect(jsonPath("_embedded.mailList[0].to", is("lfgn84@gmail.com")));



    }

    @Test
    void getMailbyID() throws Exception {
        mockMvc.perform(
                get("/api/mails/1").accept("application/hal+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("_links.self.href", is("http://localhost/api/mails/1")));

    }

    @Test
    void getCreateMail() throws Exception {
        mockMvc.perform(
                post("/api/mails/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":0,\"to\":\"lfgn84@gmail.com\",\"subject\":\"Test 4\",\"text\":\"Test Mail 4\",\"sent\": null}"))
                .andExpect(status().isCreated());

    }

   @Test
    void editMail() throws Exception{
        mockMvc.perform(
                patch("/api/mails/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":0,\"to\":\"lfgn84@gmail.com\",\"subject\":\"Test 2\",\"text\":\"Test edited Mail 2\",\"sent\": null}"))
                .andExpect(status().isOk());

    }

    @Test
    void eraseMail() throws Exception{
        mockMvc.perform(
                delete("/api/mails/3")
                .accept("application/hal+json"))
                .andExpect(status().isOk());


    }

    @Test
    void replaceMail() throws Exception {
        mockMvc.perform(
                put("/api/mails/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"to\":\"seventythree73@hotmail.com\",\"subject\":\"Test 1test\",\"text\":\"Test Mail 1test\",\"sent\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    void sendMail() throws Exception{
        mockMvc.perform(
                post("/api/mails/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":0,\"to\":\"lfgn84@gmail.com\",\"subject\":\"Test 2\",\"text\":\"Test edited Mail 2\",\"sent\":"+ new Date() +"}"))
                .andExpect(status().isOk());
    }



}

