package br.com.rentafit.migration.processor;

import br.com.rentafit.migration.dto.ClienteDocument;
import br.com.rentafit.migration.util.LegacyIdMapper;
import br.com.rentafit.people.domain.Address;
import br.com.rentafit.people.domain.Customer;
import br.com.rentafit.people.domain.Employee;
import br.com.rentafit.people.domain.PersonAddressDetails;
import br.com.rentafit.people.repository.AddressRepository;
import br.com.rentafit.people.repository.EmployeeRepository;
import br.com.rentafit.common.security.DatabaseEncryptionConverter;
import br.com.rentafit.people.util.ZipCodeUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Processor que transforma documento MongoDB Cliente → Domain Entity Customer
 *
 * Responsabilidades:
 * 1. Mapear campos de ClienteDocument para Customer entity
 * 2. Gerar novo UUID mantendo legacy_id
 * 3. Criptografar campo de documento (CPF)
 * 4. Resolver referências (endereço, funcionário criador)
 */
@Component
@RequiredArgsConstructor
public class ClienteItemProcessor implements ItemProcessor<ClienteDocument, Customer> {

    private static final Logger log = LoggerFactory.getLogger(ClienteItemProcessor.class);

    private final LegacyIdMapper legacyIdMapper;
    private final DatabaseEncryptionConverter encryptionConverter;
    private final AddressRepository addressRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public Customer process(ClienteDocument item) throws Exception {
        log.debug("Processing cliente: {}", item.getNome());

        try {
            // Gerar novo UUID
            UUID newUuid = UUID.randomUUID();

            // Extrair legacy_id do ObjectId
            Integer legacyId = legacyIdMapper.extractLegacyId(item.getId());

            // Criar entidade Customer
            Customer customer = new Customer();
            customer.setId(newUuid);
            customer.setLegacyId(legacyId);
            customer.setName(item.getNome());

            // Criptografar documento (CPF)
            if (item.getDocumento() != null) {
                String encryptedDocument = encryptionConverter.convertToDatabaseColumn(
                    item.getDocumento()
                );
                customer.setDocument(encryptedDocument);
            }

            customer.setEmail(item.getEmail());
            customer.setCreatedAt(item.getDataCriacao() != null ?
                item.getDataCriacao() : OffsetDateTime.now());
            customer.setUpdatedAt(item.getDataAtualizacao() != null ?
                item.getDataAtualizacao() : OffsetDateTime.now());

            // Campos específicos de Customer
            customer.setIsAuthenticated(item.getAutenticado() != null ?
                item.getAutenticado() : false);
            customer.setNotes(item.getNotas());

            // Processar endereço com nova estrutura
            if (item.getEndereco() != null) {
                Address address = resolveOrCreateAddress(item.getEndereco());

                PersonAddressDetails addressDetails = new PersonAddressDetails();
                addressDetails.setPerson(customer);
                addressDetails.setAddress(address);
                addressDetails.setNumber(item.getEndereco().getNumero());
                addressDetails.setComplement(item.getEndereco().getComplemento());
                addressDetails.setStartDate(OffsetDateTime.now());
                addressDetails.setEndDate(null); // Current address

                customer.setCurrentAddress(addressDetails);
            }

            // Resolver funcionário criador
            if (item.getCriadoPor() != null) {
                Integer createdByLegacyId = legacyIdMapper.extractLegacyId(item.getCriadoPor());
                Employee createdByEmployee = employeeRepository.findByLegacyId(createdByLegacyId)
                    .orElse(null);
                if (createdByEmployee != null) {
                    customer.setCreatedBy(createdByEmployee);
                }
            }

            // Adicionar telefones
            if (item.getTelefones() != null && !item.getTelefones().isEmpty()) {
                customer.setPhones(item.getTelefones());
            }

            log.debug("Successfully processed cliente: {} (legacy_id: {})",
                item.getNome(), legacyId);

            return customer;

        } catch (Exception e) {
            log.error("Error processing cliente: {}", item.getNome(), e);
            throw e;
        }
    }

    /**
     * Resolve ou cria um Address baseado nos dados do endereço
     */
    private Address resolveOrCreateAddress(ClienteDocument.EnderecoData enderecoData) {
        String normalizedZipCode = ZipCodeUtils.normalize(enderecoData.getCep());
        String street = enderecoData.getRua() != null ? enderecoData.getRua() : "Não informado";
        String city = enderecoData.getCidade() != null ? enderecoData.getCidade() : "Não informado";
        String state = enderecoData.getEstado() != null ? enderecoData.getEstado() : "SP";

        // Check if address already exists by composite key
        return addressRepository.findByZipCodeAndStreetAndCityAndState(
                        normalizedZipCode,
                        street,
                        city,
                        state)
                .orElseGet(() -> {
                    // Create new immutable address
                    Address address = new Address(
                            normalizedZipCode,
                            street,
                            enderecoData.getBairro(),
                            city,
                            state
                    );
                    return addressRepository.save(address);
                });
    }
}
