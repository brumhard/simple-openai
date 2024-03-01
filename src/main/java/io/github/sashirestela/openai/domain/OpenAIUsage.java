package io.github.sashirestela.openai.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenAIUsage {

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

}
