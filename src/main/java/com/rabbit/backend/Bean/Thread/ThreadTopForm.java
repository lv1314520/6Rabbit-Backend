package com.rabbit.backend.Bean.Thread;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class ThreadTopForm {
    @Size(min = 1, max = 20)
    private List<String> tid;

    @NotBlank
    @Range(min = 0, max = 2)
    private Integer top;
}
