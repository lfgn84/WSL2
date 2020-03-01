package se.iths.luis.webserviceslabb2;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
class MailModelAssembler implements RepresentationModelAssembler<Mail, EntityModel<Mail>> {
    @Override
    public EntityModel<Mail> toModel(Mail mail) {
        return new EntityModel<>(mail,
                linkTo(methodOn(Controller.class).one(mail.getId())).withSelfRel(),
                linkTo(methodOn(Controller.class).all()).withRel("mails"));
    }
    @Override
    public CollectionModel<EntityModel<Mail>> toCollectionModel(Iterable<? extends Mail> entities) {
        var collection = StreamSupport.stream(entities.spliterator(), false)
                .map(this::toModel)
                .collect(Collectors.toList());

        return new CollectionModel<>(collection,
                linkTo(methodOn(Controller.class).all()).withSelfRel());
    }
}





