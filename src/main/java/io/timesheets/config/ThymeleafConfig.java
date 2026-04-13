package io.timesheets.config;

import io.timesheets.util.HourFormatter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

@ControllerAdvice
public class ThymeleafConfig {

    @ModelAttribute("hourFmt")
    public HourFormatter hourFormatter() {
        return new HourFormatter();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                } else {
                    setValue(HourFormatter.parse(text));
                }
            }
        });
    }
}
