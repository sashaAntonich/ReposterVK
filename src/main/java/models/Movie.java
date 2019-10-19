package models;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Movie {
    final String proxyUrl = "http://api.scraperapi.com?api_key=4524cc553b16122c2a433229bc69248b&url=";

    String movieName;
    String movieDescription;
    String movieGenre;
    String movieRating;
    String movieDirector;

    Image movieImage;

    public Movie(int movieId) throws IOException {
        init(movieId);
    }

    public Movie(String movieName) throws IOException {
        movieName = movieName
                .replaceAll(" ", "+")
                .replaceAll(":", "%3")
                .replaceAll(",", "%2");

        Document doc = Jsoup.connect(String.format("https://www.kinopoisk.ru/index.php?kp_query=%s&what=", movieName)).get();

        if (checkCaptcha(doc)) {
            doc = Jsoup.connect(proxyUrl + String.format("https://www.kinopoisk.ru/index.php?kp_query=%s&what=", movieName)).get();
        }

        String movieIdString = doc.select("div[class='element most_wanted'] > div > p > a").get(0).attributes().get("data-id");

        init(Integer.parseInt(movieIdString));
    }

    public String getMovieName() {
        return movieName;
    }

    public String getMovieDescription() {
        return movieDescription;
    }

    public String getMovieGenre() {
        return movieGenre;
    }

    public Image getMovieImage() {
        return movieImage;
    }

    public File getMovieImageFile() throws IOException {
        File file = new File("output.jpg");
        ImageIO.write((RenderedImage) this.movieImage, "jpg", file);
        return file;
    }

    public String getMovieRating() {
        return movieRating;
    }

    public String getMovieDirector() {
        return movieDirector;
    }

    public void init(int movieId) throws IOException {
        Document doc = Jsoup.connect(String.format("https://www.kinopoisk.ru/film/%d/g", movieId)).get();

        if (checkCaptcha(doc)) {
            doc = Jsoup.connect(proxyUrl + String.format("https://www.kinopoisk.ru/film/%d/g", movieId)).get();
        }

        this.movieName = doc.select("meta[property='og:title']").get(0).attributes().get("content").replaceAll("&nbsp;", " ");

        String movieImageLink = doc.select("meta[property='og:image']").get(0).attributes().get("content");

        URL url = new URL(movieImageLink);
        this.movieImage = ImageIO.read(url);

        this.movieDescription = doc.getElementsByClass("brand_words film-synopsys").get(0).text();

        this.movieGenre = doc.select("span[itemprop='genre']").get(0).text();

        this.movieRating = doc.select("span[class='rating_ball']").get(0).text();

        this.movieDirector = doc.select("td[itemprop='director'] > a").get(0).text();
    }

    public boolean checkCaptcha(Document doc) {
        return doc.location().contains("showcaptcha");
    }
}
