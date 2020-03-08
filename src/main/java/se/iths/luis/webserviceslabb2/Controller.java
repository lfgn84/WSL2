package se.iths.luis.webserviceslabb2;

//import com.netflix.appinfo.InstanceInfo;
//import com.netflix.discovery.EurekaClient;
//import com.netflix.discovery.shared.Application;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping("/api/mails")
@Slf4j
class Controller {

//    @Qualifier("eurekaClient")
//    @Autowired
//    private EurekaClient eurekaClient;
//
//    @GetMapping("/clients")
//    public List<InstanceInfo> doRequest() {
//        Application application = eurekaClient.getApplication("MAIL-SENDER");
//        return application.getInstances();
//    }

    final MailRepository repository;
    private final MailModelAssembler assembler;
    @Autowired
    private JavaMailSender javaMailSender;
    public Controller(MailRepository repository, MailModelAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }

    @GetMapping
    public CollectionModel<EntityModel<Mail>> all() {
        log.debug("All persons called");
        return assembler.toCollectionModel(repository.findAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<EntityModel<Mail>> one(@PathVariable long id) {
        return repository.findById(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Mail> createMail(@RequestBody Mail mail) {
        log.info("POST create Mail " + mail);
        var m = repository.save(mail);
        log.info("Saved to repository " + m);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkTo(Controller.class).slash(m.getId()).toUri());
        return new ResponseEntity<>(m, headers, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteMail(@PathVariable Long id) {
        var o = repository.findById(id);
        if (repository.existsById(id) && ( o.get().getSent() == null )) {
            log.info("Mail with id = " + id + " deleted");
            repository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } else
            return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @PostMapping("/{id}")
    ResponseEntity<Mail> sendMail (@PathVariable Long id ){
        int i = id.intValue();

        if(repository.findById(id).get().getSent() == null)
            sendEmail(repository.findById(id).get());
        else
            return new ResponseEntity<>(HttpStatus.CONFLICT);

        log.info("Mail with id = "+ id +" sent to e-mail adress = " + repository.findById(id).get().getTo());
        return repository.findById(id)
                .map(mail -> {
                    mail.setSent(new Date());

                    repository.save(mail);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(linkTo(Controller.class).slash(mail.getId()).toUri());
                    return new ResponseEntity<>(mail, headers, HttpStatus.OK);
                })
                .orElseGet(() ->
                        new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    ResponseEntity<Mail> replaceMail(@RequestBody Mail newMail, @PathVariable Long id) {
        if(repository.findById(id).get().getSent() == null){
        log.info("PUT replace Mail "+ newMail);
        return repository.findById(id)
                .map(mail -> {
                    mail = newMail;
                    repository.save(mail);
                    log.info("Mail saved "+ mail);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(linkTo(Controller.class).slash(mail.getId()).toUri());
                    return new ResponseEntity<>(mail, headers, HttpStatus.OK);
                })
                .orElseGet(() ->
                        new ResponseEntity<>(HttpStatus.NOT_FOUND));
                }
        else{
            return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    @PatchMapping("/{id}")
    ResponseEntity<Mail> modifyMail(@RequestBody Mail newMail, @PathVariable Long id) {

        var o = repository.findById(id);

        if (o.isPresent() && o.get().getSent() == null) {
            var mail = o.get();
            if (mail.getSent() == null) {
                if (newMail.getTo() != null)
                    mail.setTo(newMail.getTo());
                if (newMail.getSubject() != null)
                    mail.setSubject(newMail.getSubject());
                if (newMail.getText() != null)
                    mail.setText(newMail.getText());

                log.info("PATCH edited Mail " + mail);
                var m = repository.save(mail);
                log.info("Edited and saved to repository " + m);
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(linkTo(Controller.class).slash(mail.getId()).toUri());
                return new ResponseEntity<>(mail, headers, HttpStatus.OK);
            }
        } else if (o.isPresent() && o.get().getSent() != null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
       return null;
    }

    void sendEmail(Mail mail) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(mail.getTo());

        msg.setSubject(mail.getSubject());
        msg.setText(mail.getText());

        javaMailSender.send(msg);

    }
}
