package com.telegrambot.carbot.parser;

import com.telegrambot.carbot.exception.ApiException;
import com.telegrambot.carbot.model.Car;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class HasznaltautoParser {

    private final static String URL = "https://www.hasznaltauto.hu/szemelyauto/";
    private final static String DIV_LIST_VIEW = "div.list-view";
    private final static String PAGE = "page";
    private final static String SID = "sid";
    private final static String HREF = "href";
    private final static String LI_NEXT = "li.next";
    private final static String LI_NEXT_DISABLED = "li.next.disabled";

    public Set<Car> parse(String make, String model) {
        Set<Car> cars = new HashSet<>();
        try {
            int currentPage = 1;
            boolean hasNextPage = true;

            while (hasNextPage) {
                String url = URL + make + "/" + model + "/" + PAGE + currentPage;
                Document doc = Jsoup.connect(url).get();
                Elements carElements = doc.select(DIV_LIST_VIEW);
                carElements.stream()
                        .map(Element::getAllElements)
                        .flatMap(Collection::stream)
                        .map(element -> element.getElementsByAttribute(HREF).attr(HREF))
                        .filter(carLink -> carLink.contains(SID))
                        .map(carLink -> StringUtils.substringBefore(carLink, "#"))
                        .map(cutLink -> new Car(make, model, cutLink))
                        .forEach(cars::add);

                Element nextPage = doc.selectFirst(LI_NEXT);
                Element nextPageDisabled = doc.selectFirst(LI_NEXT_DISABLED);
                if (nextPage != null && nextPageDisabled == null) {
                    currentPage++;
                } else {
                    hasNextPage = false;
                }
            }
        } catch (IOException e) {
            log.error("Error during parsing " + URL + ": {}", e.getMessage());
            throw new ApiException("Error during parsing  + URL + : {}", e);
        }
        return cars;
    }
}
