package gov.va.api.health.ids.service.controller;

import gov.va.api.health.ids.api.IdentityService.UnknownIdentity;
import gov.va.api.health.ids.api.Registration;
import gov.va.api.health.ids.api.ResourceIdentity;
import gov.va.api.health.ids.service.controller.impl.ResourceIdentityDetail;
import gov.va.api.health.ids.service.controller.impl.ResourceIdentityDetailRepository;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
@AllArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class IdServiceV1ApiController {
  private final ResourceIdentityDetailRepository repository;
  private final UuidGenerator uuidGenerator;

  private List<Registration> find(ResourceIdentity identity) {
    List<ResourceIdentityDetail> previouslyRegistered =
        repository.findBySystemAndResourceAndIdentifier(
            identity.system(), identity.resource(), identity.identifier());
    return previouslyRegistered.stream().map(this::toRegistration).collect(Collectors.toList());
  }

  /** Implementation of GET /v1/ids/{publicId}. See api-v1.yaml. */
  @RequestMapping(
      value = {"/v1/ids/{publicId}", "/resourceIdentity/{publicId}"},
      produces = {"application/json"},
      method = RequestMethod.GET)
  @SneakyThrows
  public ResponseEntity<List<ResourceIdentity>> lookup(
      @Valid @PathVariable("publicId") @Pattern(regexp = "[-A-Za-z0-9]+") String publicId) {
    List<ResourceIdentity> identities =
        repository.findByUuid(publicId).stream()
            .map(ResourceIdentityDetail::asResourceIdentity)
            .collect(Collectors.toList());
    log.info("Found {} identities for {}", identities.size(), safe(publicId));

    if (identities.isEmpty()) {
      throw new UnknownIdentity(publicId);
    }

    return ResponseEntity.ok().body(identities);
  }

  /** Implementation of POST /v1/ids. See api-v1.yaml. */
  @RequestMapping(
      value = {"/v1/ids", "/resourceIdentity"},
      produces = {"application/json"},
      consumes = {"application/json"},
      method = RequestMethod.POST)
  public ResponseEntity<List<Registration>> register(
      @Valid @RequestBody List<ResourceIdentity> identities) {
    List<Registration> registrations = new LinkedList<>();
    List<ResourceIdentityDetail> newRegistrations = new LinkedList<>();

    for (ResourceIdentity identity : identities) {
      List<Registration> previouslyRegistered = find(identity);
      if (previouslyRegistered.isEmpty()) {
        ResourceIdentityDetail databaseEntry = toDatabaseEntry(identity);
        newRegistrations.add(databaseEntry);
        registrations.add(toRegistration(databaseEntry));
      } else {
        registrations.addAll(previouslyRegistered);
      }
    }
    repository.saveAll(newRegistrations);

    log.info("Register {} entries ({} are new)", identities.size(), newRegistrations.size());
    return ResponseEntity.status(HttpStatus.CREATED).body(registrations);
  }

  /** Sanitize strings to prevent log forgery. */
  private String safe(String value) {
    if (value == null) {
      return null;
    }
    return value.replaceAll("[\\s\r\n]", "");
  }

  private ResourceIdentityDetail toDatabaseEntry(ResourceIdentity resourceIdentity) {
    return ResourceIdentityDetail.builder()
        .uuid(uuidGenerator.apply(resourceIdentity))
        .system(resourceIdentity.system())
        .resource(resourceIdentity.resource())
        .identifier(resourceIdentity.identifier())
        .build();
  }

  private Registration toRegistration(ResourceIdentityDetail resourceIdentityDetail) {
    return Registration.builder()
        .uuid(resourceIdentityDetail.uuid())
        .resourceIdentity(resourceIdentityDetail.asResourceIdentity())
        .build();
  }

  /**
   * Generates consistent public UUIDs for a given resource identity. This function should be
   * deterministic. Failure to do so will result in multiple registrations for the same identity.
   */
  public interface UuidGenerator extends Function<ResourceIdentity, String> {}
}
