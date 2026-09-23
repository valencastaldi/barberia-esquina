package com.barberiaesquina.turnos.web;

import com.barberiaesquina.turnos.modelo.ValorEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Locale;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    /** Los enums en la URL se escriben en minúscula: ?estado=pendiente, ?filtro=frecuentes. */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new EnumsSinMayusculas());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class EnumsSinMayusculas implements ConverterFactory<String, Enum> {
        @Override
        public <T extends Enum> Converter<String, T> getConverter(Class<T> tipo) {
            return texto -> {
                if (texto == null || texto.isBlank()) return null;
                if (ValorEnum.class.isAssignableFrom(tipo)) {
                    return (T) ValorEnum.desde((Class) tipo, texto);
                }
                return (T) Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
            };
        }
    }
}
