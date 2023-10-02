package com.springia.springiademo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.client.AiClient;
import org.springframework.ai.client.AiResponse;
import org.springframework.ai.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoRestController {

    private final AiClient aiClient;
    @Value("${openai.api-key}")
    private String apiKey;

    public DemoRestController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/joke")
    public String getJoke(@RequestParam(name = "topic") String topic) {
        PromptTemplate promptTemplate = new PromptTemplate("""
                Je m'ennuie avec les applications hello world. Et si vous me faisiez une blague sur {sujet} ? pour commencer ?
                Incluez quelques termes de programmation dans votre blague pour la rendre plus amusante..
                """);
        promptTemplate.add("topic", topic);
        return this.aiClient.generate(promptTemplate.create()).getGeneration().getText();
    }

    @GetMapping("/bestMovie")
    public String getBestMovie(@RequestParam(name = "genre") String genre, @RequestParam(name = "year") String year) {
        PromptTemplate promptTemplate = new PromptTemplate("""
                Je m'ennuie avec les applications hello world. Et si vous me donniez un film sur {genre} dans {année} pour commencer ?
                Mais choisissez le meilleur film possible. Je suis un critique de cinéma, après tout. Les classements IMDB sont un bon point de départ.
                Quel est l'acteur ou l'actrice qui joue dans ce film ? Qui l'a réalisé ? Et qui l'a écrit ? Pouvez-vous me donner un bref résumé de l'intrigue et le nom du film ?
                Mais ne me donnez pas trop d'informations. Je veux être surpris.
                Et s'il vous plaît, donnez-moi ces détails dans le format JSON suivant : genre, année, nom du film, acteur, réalisateur, scénariste, intrigue.""");
        Map.of("genre", genre, "year", year).forEach(promptTemplate::add);
        AiResponse generate = this.aiClient.generate(promptTemplate.create());
        return generate.getGeneration().getText();
    }

    @GetMapping(value = "/image", produces = "image/jpeg")
    public ResponseEntity<InputStreamResource> getImage(@RequestParam(name = "topic") String topic) throws URISyntaxException {
        PromptTemplate promptTemplate = new PromptTemplate("""
                I'm bored with hello world apps. Can you create me a prompt about {topic}. Enhance the topic I gave you. Make it fancy.
                Make resolution 256x256 but in Json it needs to be string. I want only 1 creation. Give me as JSON format: prompt, n, size.
                Do not make any comments. Just JSON file.
                """);
        promptTemplate.add("topic", topic);
        String imagePrompt = this.aiClient.generate(promptTemplate.create()).getGeneration().getText();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("Content-Type", "application/json");
        HttpEntity<String> httpEntity = new HttpEntity<>(imagePrompt,headers);

        String imageUrl = restTemplate.exchange("https://api.openai.com/v1/images/generations", HttpMethod.POST, httpEntity, GeneratedImage.class)
                .getBody().getData().get(0).getUrl();
        byte[] imageBytes = restTemplate.getForObject(new URI(imageUrl), byte[].class);
        return ResponseEntity.ok().body(new InputStreamResource(new java.io.ByteArrayInputStream(imageBytes)));
    }

}
