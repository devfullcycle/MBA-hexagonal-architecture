package br.com.fullcycle.hexagonal.infrastructure.controllers;

import br.com.fullcycle.hexagonal.infrastructure.dtos.PartnerDTO;
import br.com.fullcycle.hexagonal.infrastructure.repositories.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
public class PartnerControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PartnerRepository partnerRepository;

    @AfterEach
    void tearDown() {
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve criar um parceiro")
    public void testCreate() throws Exception {

        var partner = new PartnerDTO();
        partner.setCnpj("41536538000100");
        partner.setEmail("john.doe@gmail.com");
        partner.setName("John Doe");

        final var result = this.mvc.perform(
                        MockMvcRequestBuilders.post("/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(partner))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().exists("Location"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsByteArray();

        var actualResponse = mapper.readValue(result, PartnerDTO.class);
        Assertions.assertEquals(partner.getName(), actualResponse.getName());
        Assertions.assertEquals(partner.getCnpj(), actualResponse.getCnpj());
        Assertions.assertEquals(partner.getEmail(), actualResponse.getEmail());
    }

    @Test
    @DisplayName("Não deve cadastrar um parceiro com CNPJ duplicado")
    public void testCreateWithDuplicatedCPFShouldFail() throws Exception {

        var partner = new PartnerDTO();
        partner.setCnpj("41536538000100");
        partner.setEmail("john.doe@gmail.com");
        partner.setName("John Doe");

        // Cria o primeiro parceiro
        this.mvc.perform(
                        MockMvcRequestBuilders.post("/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(partner))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().exists("Location"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsByteArray();

        partner.setEmail("john2@gmail.com");

        // Tenta criar o segundo parceiro com o mesmo CPF
        this.mvc.perform(
                        MockMvcRequestBuilders.post("/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(partner))
                )
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.content().string("Partner already exists"));
    }

    @Test
    @DisplayName("Não deve cadastrar um parceiro com e-mail duplicado")
    public void testCreateWithDuplicatedEmailShouldFail() throws Exception {

        var partner = new PartnerDTO();
        partner.setCnpj("41536538000100");
        partner.setEmail("john.doe@gmail.com");
        partner.setName("John Doe");

        // Cria o primeiro parceiro
        this.mvc.perform(
                        MockMvcRequestBuilders.post("/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(partner))
                )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().exists("Location"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsByteArray();

        partner.setCnpj("66666538000100");

        // Tenta criar o segundo parceiro com o mesmo CNPJ
        this.mvc.perform(
                        MockMvcRequestBuilders.post("/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(partner))
                )
                .andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
                .andExpect(MockMvcResultMatchers.content().string("Partner already exists"));
    }

    @Test
    @DisplayName("Deve obter um parceiro por id")
    public void testGet() throws Exception {

        var partner = new PartnerDTO();
        partner.setCnpj("41536538000100");
        partner.setEmail("john.doe@gmail.com");
        partner.setName("John Doe");

        final var createResult = this.mvc.perform(
                        MockMvcRequestBuilders.post("/partners")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(partner))
                )
                .andReturn().getResponse().getContentAsByteArray();

        var partnerId = mapper.readValue(createResult, PartnerDTO.class).getId();

        final var result = this.mvc.perform(
                        MockMvcRequestBuilders.get("/partners/{id}", partnerId)
                )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        var actualResponse = mapper.readValue(result, PartnerDTO.class);
        Assertions.assertEquals(partnerId, actualResponse.getId());
        Assertions.assertEquals(partner.getName(), actualResponse.getName());
        Assertions.assertEquals(partner.getCnpj(), actualResponse.getCnpj());
        Assertions.assertEquals(partner.getEmail(), actualResponse.getEmail());
    }
}
