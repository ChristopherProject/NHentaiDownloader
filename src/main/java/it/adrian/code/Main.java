package it.adrian.code;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String BASE_PATH = "https://nhentai.to";
    private static final String BASE_PATH_HENTAI_PAGE = "https://nhentai.to/g";
    private static final String STORAGE_PATH = "https://img.dogehls.xyz//galleries";
    private static Scanner scanner;

    public static void main( String[] args ) {
        scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1) Download Hentai By ID");
            System.out.println("2) Get All IDs From Category");
            System.out.println("3) Download All From Category");
            System.out.println("4) Esci");
            Action action = getAction();

            switch (action) {
                case DOWNLOAD_BY_ID:
                    downloadById();
                    break;
                case DOWNLOAD_BY_CATEGORY:
                    downloadByCategory();
                    break;
                case DOWNLOAD_ALL_FROM_CATEGORY:
                    downloadAllFromCategory();
                    break;
                case EXIT:
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid Selection");
                    break;
            }
        }
    }

    private static Action getAction() {
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                return Action.DOWNLOAD_BY_ID;
            case 2:
                return Action.DOWNLOAD_BY_CATEGORY;
            case 3:
                return Action.DOWNLOAD_ALL_FROM_CATEGORY;
            case 4:
                return Action.EXIT;
            default:
                return null;
        }
    }

    private static void downloadById() {
        System.out.println("Insert ID of your hentai:");
        int id = scanner.nextInt();
        scanner.nextLine();
        createPDF(id);
    }

    private static void downloadByCategory() {
        System.out.println("Insert your target category:");
        String category = scanner.nextLine();
        getAllIDFromListOfLinks(getAllLinksFromCategory(category)).forEach(System.out::println);
    }

    private static void downloadAllFromCategory() {
        System.out.println("Insert your target category:");
        String category = scanner.nextLine();
        downloadAllFromCateogory(category);
    }

    public static List<Integer> getAllIDFromListOfLinks( List<String> result ) {
        List<Integer> idList = new ArrayList<>();
        if (!result.isEmpty()) {
            for (String res : result) {
                int id = Integer.parseInt(res.replace("https://nhentai.to/g/", "").replace("/", ""));
                idList.add(id);
            }
        }
        return idList;
    }

    public static void downloadAllFromCateogory( String search ) {
        System.out.println(getAllResultFromResearch(search));
        List<String> result = getAllLinksFromCategory(search);
        List<Integer> idList = new ArrayList<>();
        if (!result.isEmpty()) {
            for (String res : result) {
                int id = Integer.parseInt(res.replace("https://nhentai.to/g/", "").replace("/", ""));
                idList.add(id);
            }
        }
        for (Integer id : idList) {
            createPDF(id);
        }
        idList.clear();
    }

    public static List<String> getAllLinksFromCategory( String category ) {
        List<String> fuck = new ArrayList<>();
        try {
            String url = BASE_PATH + "/search?q=" + category;
            Document doc = Jsoup.connect(url).get();
            Elements all = doc.select("a[href]");
            for (Element element : all) {
                if (element.attr("href").startsWith("/g/")) {
                    fuck.add(BASE_PATH_HENTAI_PAGE.replace("/g", "") + element.attr("href"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fuck;
    }

    public static String getAllResultFromResearch( String category ) {
        try {
            String url = BASE_PATH + "/search?q=" + category;
            Document doc = Jsoup.connect(url).get();
            Element sex = doc.select("h2").first();
            return sex.toString().replace("<h2>", "").replace("</h2>", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getTitleFromID( int hentai_id ) {
        try {
            String url = BASE_PATH_HENTAI_PAGE + "/" + hentai_id;
            Document doc = Jsoup.connect(url).get();
            Element sex = doc.select("h1").first();
            return sex.toString().replace("<h1>", "").replace("</h1>", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void createPDF( int hentai_id ) {
        PDDocument document = new PDDocument();
        List<String> imageUrls = getAllPagesLinks(hentai_id);
        if (imageUrls.isEmpty()) return;
        try {
            PDPageContentStream contentStream = null;
            PDPage currentPage = null;
            float y = 0;
            for (String url : imageUrls) {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
                connection.setRequestMethod("GET");
                BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
                if (currentPage == null || y + pdImage.getHeight() > currentPage.getMediaBox().getHeight()) {
                    currentPage = new PDPage();
                    document.addPage(currentPage);
                    if (contentStream != null) {
                        contentStream.close();
                    }
                    contentStream = new PDPageContentStream(document, currentPage);
                    y = 0;
                }
                float scaling = (currentPage.getMediaBox().getWidth() - 40) / pdImage.getWidth();
                if (currentPage != null) {
                    contentStream.drawImage(pdImage, 20, y, pdImage.getWidth() * scaling, pdImage.getHeight() * scaling);
                    y += pdImage.getHeight() * scaling + 20;
                }
            }
            if (contentStream != null) {
                contentStream.close();
            }
            document.save(getTitleFromID(hentai_id) + ".pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> getAllPagesLinks( int hentai_id ) {
        List<String> pages = new ArrayList<>();
        int n1 = getGalleryID(hentai_id), n2 = getNumberOfPages(hentai_id);
        if (n1 != 0 && n2 != 0) {
            pages = getPagesLink(n1, n2);
        }
        return pages;
    }

    public static String getCoverLink( int gallery_id ) {
        return STORAGE_PATH + "/" + gallery_id + "/cover.jpg";
    }

    public static List<String> getPagesLink( int gallery_id, int numberOfPages ) {
        List<String> pages = new ArrayList<>();
        for (int i = 1; i < numberOfPages; i++) {
            pages.add(STORAGE_PATH + "/" + gallery_id + "/" + i + ".jpg");
        }
        return pages;
    }

    public static int getGalleryID( int hentai_id ) {
        try {
            String url = BASE_PATH_HENTAI_PAGE + "/" + hentai_id;
            Document doc = Jsoup.connect(url).get();
            Elements imgElements = doc.select("img");
            Element element = null;
            for (Element imgElement : imgElements) {
                String src = imgElement.attr("src");
                if (src.contains("/galleries/") && src.endsWith("/cover.jpg")) {
                    element = imgElement;
                    break;
                }
            }
            if (element == null) {
                System.err.println("Cover image not found for hentai_id " + hentai_id);
                return 0;
            }
            return Integer.parseInt(element.attr("src").replace("https://img.dogehls.xyz/galleries/", "").replace("/cover.jpg", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getNumberOfPages( int hentai_id ) {
        try {
            String url = BASE_PATH_HENTAI_PAGE + "/" + hentai_id;
            Document doc = Jsoup.connect(url).get();
            Elements spanElement = doc.select("span.name");
            Element element = spanElement.get(spanElement.size() - 1);
            return Integer.parseInt(element.toString().replace("<span class=\"name\">", "").replace("</span>", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }


    public enum Action {
        DOWNLOAD_BY_ID, DOWNLOAD_BY_CATEGORY, DOWNLOAD_ALL_FROM_CATEGORY, EXIT
    }
}