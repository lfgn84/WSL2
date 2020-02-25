package se.iths.luis.webserviceslabb2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mails")
@Slf4j
class Controller {

    final MailRepository repository;

    //  Mail mail1 = new Mail(1,"lfgn84@gmail.com","First mail", "This is the firs e-mail.");

    public Controller(MailRepository repository) {
        this.repository = repository;
    }


    @GetMapping
    public List<Mail> allMails() {
        log.debug("All mails are called");
        return repository.findAll();

    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Mail> oneMail(@PathVariable long id) {
        var mailOptional = repository.findById(id);

        return mailOptional.map(mail -> new ResponseEntity<>(mail, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));

    }

    @PostMapping
    public ResponseEntity<Mail> createMail(@RequestBody Mail mail) {
        log.info("POST create Mail " + mail);
        var m = repository.save(mail);
        log.info("Saved to repository " + m);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/api/mails/" + m.getId());
        return new ResponseEntity<>(m, headers, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteMail(@PathVariable Long id) {
        if (repository.existsById(id)) {
            log.info("Mail deleted");
            repository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{id}")
    public void sendOneMail(@PathVariable Long id){
        int i = id.intValue();
        sendEmail(repository.getOne(id));
        log.info("Mail with id = "+ id +" sent to e-mail adress = " + repository.getOne(id).getTo());
    }



    @Autowired
    private JavaMailSender javaMailSender;

    void sendEmail(Mail mail) {

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(mail.getTo());

        msg.setSubject(mail.getSubject());
        msg.setText(mail.getText());

        javaMailSender.send(msg);

    }

}
