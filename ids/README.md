# ids - Identity Service

The ID service provides a mechanism to translate identifiers that should remain internal or secret
to the system with equivalent Identities that are public. 

## Data Model

_Resource Identity_
- Consists of a system, resource type, and an identity.
- The _identity_ is unique for a given system and resource combination. 

_Registration_
- Consists of a generated UUID, and list of resource identities.
- The generated UUID will be considered the public ID.
- ⚠ The list of resource identities currently contains only one item. This structure
  will eventually support associated system, e.g. CDW and Cerner.

## Behavior

```
GIVEN a list of unregistered resource identities 
WHEN a register request is made,
THEN a 201 response will be returned with a list of registrations, one per given resource identity.

The UUID will be considered public and can be used to perform lookup requests in future calls.
```

```
Registration is idempotent. 
GIVEN a list of previously registered resource identities,
WHEN a register request is made, 
THEN a 201 response will be returned with a list of previously registered registrations. 

The UUID for the resource identity is not regenerated
Duplicate resource identities are not associated with the UUID.
```

```
GIVEN a previously registered public ID,
WHEN a lookup request is made,
THEN a 200 response is returned with all registered identities as the body.
```

```
GIVEN a unregistered public ID,
WHEN a lookup request is made,
THEN a 404 response is returned.
```
