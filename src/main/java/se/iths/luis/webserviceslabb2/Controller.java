package se.iths.luis.webserviceslabb2;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
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

    @Qualifier("eurekaClient")
    @Autowired
    private EurekaClient eurekaClient;

    @GetMapping("/clients")
    public List<InstanceInfo> doRequest() {
        Application application = eurekaClient.getApplication("MAIL-SENDER");
        return application.getInstances();
    }

    final MailRepository repository;
    private final MailModelAssembler assembler;

    //  Mail mail1 = new Mail(1,"lfgn84@gmail.com","First mail", "This is the firs e-mail.");

    public Controller(MailRepository repository, MailModelAssembler assembler) {
        this.repository = repository;
        this.assembler = assembler;
    }
    @GetMapping
    public CollectionModel<EntityModel<Mail>> all() {
        log.debug("All persons called");
        return assembler.toCollectionModel(repository.findAll());
    }

//    @GetMapping
//    public List<Mail> allMails() {
//        log.debug("All mails are called");
//        return repository.findAll();
//    }
    @GetMapping(value = "/{id}")
    public ResponseEntity<EntityModel<Mail>> one(@PathVariable long id) {
        return repository.findById(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
//    @GetMapping(value = "/{id}")
//    public ResponseEntity<Mail> oneMail(@PathVariable long id) {
//        var mailOptional = repository.findById(id);
//
//        return mailOptional.map(mail -> new ResponseEntity<>(mail, HttpStatus.OK))
//                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
//    }

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
        if (repository.existsById(id) && ( repository.getOne(id).getSent() == null )) {
            log.info("Mail deleted");
            repository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } else
            return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @PostMapping("/{id}")
    ResponseEntity<Mail> sendMail (@PathVariable Long id ){
        int i = id.intValue();

        if(repository.getOne(id).getSent() == null)
            sendEmail(repository.getOne(id));
        else
            return new ResponseEntity<>(HttpStatus.CONFLICT);

        log.info("Mail with id = "+ id +" sent to e-mail adress = " + repository.getOne(id).getTo());
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
        if(repository.getOne(id).getSent() == null){
        return repository.findById(id)
                .map(mail -> {
                    mail = newMail;
                    repository.save(mail);
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
        if(repository.getOne(id).getSent() == null){
        return repository.findById(id)
                .map(mail -> {
                    if (newMail.getTo() != null)
                        mail.setTo(newMail.getTo());
                    if(newMail.getSubject() != null)
                        mail.setSubject(newMail.getSubject());
                    if(newMail.getText() != null)
                        mail.setText(newMail.getText());

                    log.info("PATCH edited Mail " + mail);
                    var m = repository.save(mail);
                    log.info("Edited and saved to repository " +  m);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(linkTo(Controller.class).slash(mail.getId()).toUri());
                    return new ResponseEntity<>(mail, headers, HttpStatus.OK);
                    }
                 )
                .orElseGet(() ->
                        new ResponseEntity<>(HttpStatus.NOT_FOUND));}
        else{
            return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
        }
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
