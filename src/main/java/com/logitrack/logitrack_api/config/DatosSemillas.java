package com.logitrack.logitrack_api.config;

import com.logitrack.logitrack_api.model.Envio;
import com.logitrack.logitrack_api.model.EstadoEnvio;
import com.logitrack.logitrack_api.model.Usuario;
import com.logitrack.logitrack_api.repository.EnvioRepository;
import com.logitrack.logitrack_api.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class DatosSemillas {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Bean
    CommandLineRunner initDatabase(EnvioRepository repository, UsuarioRepository usuarioRepository) {
        return args -> {

            if (usuarioRepository.count() == 0) {
                Usuario melina = new Usuario();
                melina.setUsuario("melina");
                melina.setClave(encoder.encode("1234"));
                melina.setNombre("Melina");
                melina.setApellido("Scabini");
                melina.setRol("Operador");

                Usuario ciro = new Usuario();
                ciro.setUsuario("ciro");
                ciro.setClave(encoder.encode("admin"));
                ciro.setNombre("Ciro");
                ciro.setApellido("López");
                ciro.setRol("Supervisor");

                usuarioRepository.save(melina);
                usuarioRepository.save(ciro);
            }

            if (repository.count() == 0) {

                Envio e1 = new Envio();
                e1.setTrackingId(UUID.randomUUID().toString());
                e1.setDni("40123456");
                e1.setNombre("Karin");
                e1.setPrioridad("MEDIA");
                e1.setApellido("Pellegrini");
                e1.setDireccion("Av Siempre Viva 742");
                e1.setCodigoPostalDestino("1665");
                e1.setCodigoPostalOrigen("1667");
                e1.setPeso(2.5);
                e1.setTipoEnvio("Estandar");
                e1.setDistanciaKm(12.4);
                e1.setMotivoPrioridad("Peso moderado (2.5 kg) a distancia intermedia (12.4 km). Prioridad estándar.");
                e1.setEstado(EstadoEnvio.REGISTRADO);
                e1.setFechaCreacion(LocalDateTime.now().minusDays(5));

                Envio e2 = new Envio();
                e2.setTrackingId(UUID.randomUUID().toString());
                e2.setDni("38999888");
                e2.setNombre("Ciro");
                e2.setPrioridad("BAJA");
                e2.setApellido("Lopez");
                e2.setDireccion("Calle Falsa 123");
                e2.setCodigoPostalDestino("1614");
                e2.setCodigoPostalOrigen("1667");
                e2.setPeso(1.2);
                e2.setTipoEnvio("Fragil");
                e2.setDistanciaKm(38.7);
                e2.setMotivoPrioridad("Envío liviano (1.2 kg) de corta distancia (38.7 km). Sin urgencia especial.");
                e2.setEstado(EstadoEnvio.EN_TRANSITO);
                e2.setFechaCreacion(LocalDateTime.now().minusDays(3));

                Envio e3 = new Envio();
                e3.setTrackingId(UUID.randomUUID().toString());
                e3.setDni("35555111");
                e3.setNombre("Melina");
                e3.setPrioridad("ALTA");
                e3.setApellido("Scabini");
                e3.setDireccion("San Martin 550");
                e3.setCodigoPostalDestino("2000");
                e3.setCodigoPostalOrigen("1667");
                e3.setPeso(3.0);
                e3.setTipoEnvio("Medica");
                e3.setDistanciaKm(285.3);
                e3.setMotivoPrioridad("Envío de tipo médico/urgente (3.0 kg) con destino lejano (285.3 km). Requiere atención prioritaria.");
                e3.setEstado(EstadoEnvio.EN_SUCURSAL);
                e3.setFechaCreacion(LocalDateTime.now().minusDays(1));

                repository.save(e1);
                repository.save(e2);
                repository.save(e3);
            }
        };
    }
}