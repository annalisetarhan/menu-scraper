package com.annalisetarhan.menuscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.time.LocalDate;

public class MenuScraper {
    private static String millenniumMenu = "https://www.millenniumrestaurant.com/menu";
    private static String graciasMadreMenu = "https://www.up2datemenu.com/plain_menu?menu_id=";
    private static int[] graciasMadreIDs = {9, 14, 18, 19, 34};
    private static String nativeFoodsPage = "https://www.nativefoods.com/our-menu";
    private static String veggieGrillPage = "https://www.veggiegrill.com/assets/392/src/MenuPDF.pdf";
    private static String biNeviDeliPage = "https://binevideli.com/en/menus/";

    public static void main(String[] args) {
        System.out.println("Creating this month's directory...\n");

        // Creates a directory based on current date, e.g. June2019
        // If directory can't be created, ends program early

        LocalDate date = LocalDate.now();
        String month = date.getMonth().toString().toLowerCase();
        month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
        String dateString = month + "" + date.getYear();
        String fileString = "/Users/annalisetarhan/Desktop/ScrapedMenus/" + dateString;

        try {
            File directory = new File(fileString);
            if (!directory.exists()) {
                directory.mkdir();
            }
        } catch (Exception e) {
            System.out.println("This month's directory was not created. Try again next month.");
            e.printStackTrace();
            return;
        }

        // Saves each restaurant's menu in the new directory
        System.out.println("Fetching Millennium's menu...");
        saveMillenniumMenu(fileString);

        System.out.println("Fetching Kindred's menu...");
        saveKindredMenu(fileString);

        System.out.println("Fetching Gracias Madre's menu...");
        saveGraciasMadreMenu(fileString);

        System.out.println("Fetching Native Foods' menu...");
        saveNativeFoodsMenu(fileString);

        /* Veggie Grill doesn't cooperate anymore
        System.out.println("Fetching Veggie Grill's menu...");
        saveVeggieGrillMenu(fileString);
         */

        System.out.println("Fetching Bi Nevi Deli's menu...");
        saveBiNeviDeliMenu(fileString);

        System.out.println("\nThank you, come again!");
    }

    /*
     * Accesses Millennium's menu and uses Jsoup to save entire page as a txt file
     */

    private static void saveMillenniumMenu(String fileString) {
        Document doc;

        try {
            doc = Jsoup.connect(millenniumMenu).get();
        } catch (IOException e) {
            System.out.println("Millennium's menu could not be reached.");
            e.printStackTrace();
            return;
        }

        cleanMillennium(doc, fileString);
    }

    /*
     * Extracts basic information from Millennium menu and saves as a txt file
     */

    private static void cleanMillennium(Document doc, String fileString) {
        String fileName = fileString + "//" + "MillenniumClean.txt";

        try {
            BufferedWriter cleanWriter = new BufferedWriter(new FileWriter(fileName));

            // Writes menu's title

            String titleString = doc.selectFirst("title").text();
            cleanWriter.write(titleString);
            cleanWriter.newLine();
            cleanWriter.newLine();

            // Writes names of chefs

            Elements chefs = doc.select("p");
            String chefString = chefs.first().text();
            cleanWriter.write(chefString);
            cleanWriter.newLine();
            chefString = chefs.first().nextElementSibling().text();
            cleanWriter.write(chefString);

            // Iterates through all elements on page, only storing menu items from interesting sections

            Elements allElements = doc.getAllElements();

            ArrayList<String> relevantSections = new ArrayList<>();
            relevantSections.add("Starters");
            relevantSections.add("Mains");
            relevantSections.add("Cocktails");
            relevantSections.add("DESSERTS");
            relevantSections.add("Sundays from 10:30am-2:00pm");

            boolean sectionIsRelevant = true;

            for (Element e : allElements) {
                if (e.className().equals("menu-section-title")) {
                    if (relevantSections.contains(e.ownText())) {
                        cleanWriter.newLine();
                        cleanWriter.newLine();
                        cleanWriter.newLine();
                        cleanWriter.write(e.ownText());
                        cleanWriter.newLine();
                        cleanWriter.newLine();
                        sectionIsRelevant = true;
                    } else {
                        sectionIsRelevant = false;
                    }
                } else if (sectionIsRelevant && e.className().equals("menu-item-title")) {
                    cleanWriter.newLine();
                    cleanWriter.write(e.ownText());
                    cleanWriter.newLine();
                } else if (sectionIsRelevant && e.className().equals("menu-item-description")) {
                    cleanWriter.write(e.ownText());
                    cleanWriter.newLine();
                }
            }
            cleanWriter.close();

        } catch (IOException e) {
            System.out.println("Millennium's cleaned up menu could not be written.");
            e.printStackTrace();
        }
    }

    /*
     * Accesses Gracias Madre's menu and uses Jsoup to save it as a reasonably well formatted txt file
     */

    private static void saveGraciasMadreMenu(String fileString) {
        String fileName = "GraciasMadre.txt";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileString + "//" + fileName));

            /*
            Menu is spread across multiple pages with urls differing only by final numerical id.
	        IDs stored in "graciasMadreIDs" array. Compiles all pages to a single text file.
	        This is more robust than the strategy I used for cleanMillennium(), since I use Jsoup.clean()
	        instead of looking for specific data.
	        */

            for (int i : graciasMadreIDs) {
                String graciasMadreURL = graciasMadreMenu + i;
                Document doc = Jsoup.connect(graciasMadreURL).get();
                Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
                doc.outputSettings(settings);

                String docBody = doc.body().toString();
                int startIndex = docBody.indexOf("<h3 style=\"font-family: 'Piedra'");
                int endIndex = docBody.length();

                if (docBody.contains("\t<div id=\"top_button\">\n")) {
                    endIndex = docBody.indexOf("\t<div id=\"top_button\">\n");
                }

                docBody = docBody.substring(startIndex, endIndex);

                String menuText = Jsoup.clean(docBody, graciasMadreURL, Whitelist.none(), settings);
                writer.write(menuText);
            }
            writer.close();

        } catch (IOException e) {
            System.out.println("Something went wrong with Gracias Madre's menu.");
            e.printStackTrace();
        }
    }

    /*
     * Accesses Kindred's menu and saves as a pdf
     */

    private static void saveKindredMenu(String fileString) {
        String fileName = "Kindred.pdf";
        URL kindredMenuURL = null;

        try {
            kindredMenuURL = new URL("https://cdn.shopify.com/s/files/1/0405/4636/9692/files/2020_KINDRED_Menu.pdf?v=1601336865");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        assert kindredMenuURL != null;
        pdfMenuHelper(kindredMenuURL, fileString, fileName);
    }

    /*
     * Accesses Native Foods' menu and saves as a pdf
     */

    private static void saveNativeFoodsMenu(String fileString) {
        String fileName = "NativeFoods.pdf";
        URL nativeFoodsMenuURL = null;

        try {
            Document doc = Jsoup.connect(nativeFoodsPage).get();
            boolean chooseNext = false;
            Elements links = doc.select("a[href]");

            // Depends on actual menu link being second "menu" link on page.

            for (Element link : links) {
                if (link.text().equals("MENU")) {
                    if (!chooseNext) {
                        chooseNext = true;
                    } else {
                        nativeFoodsMenuURL = new URL(link.attr("abs:href"));
                        break;
                    }

                }
            }
        } catch (IOException e) {
            System.out.println("Oops. Couldn't find Native Foods' menu.");
            e.printStackTrace();
            return;
        }
        assert nativeFoodsMenuURL != null;
        pdfMenuHelper(nativeFoodsMenuURL, fileString, fileName);
    }

    /*
     * Accesses Veggie Grill's menu and saves as a pdf
     */

    private static void saveVeggieGrillMenu(String fileString) {
        String fileName = "VeggieGrill.pdf";
        URL veggieGrillMenuURL;

        try {
            veggieGrillMenuURL = new URL(veggieGrillPage);
        } catch (MalformedURLException e) {
            System.out.println("Problem with Veggie Grill's URL");
            e.printStackTrace();
            return;
        }

        pdfMenuHelper(veggieGrillMenuURL, fileString, fileName);
    }

    /*
     * Accesses Bi Nevi Deli's menu and concatenates jpgs
     */

    private static void saveBiNeviDeliMenu(String fileString) {
        String fileName = fileString + "//" + "BiNeviDeli.png";
        try {
            Document doc = Jsoup.connect(biNeviDeliPage).get();
            Element imageWrapper = doc.selectFirst("div.vc_single_image-wrapper");
            String menuUrl = imageWrapper.selectFirst("img[src$=.png]").attr("src");
            BufferedImage image = ImageIO.read(new URL(menuUrl));
            File file = new File(fileName);
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            System.out.println("Choked on BiNevi :(");
            e.printStackTrace();
        }
    }

    /*
     * Helper method for pdf menus
     */

    private static void pdfMenuHelper(URL url, String fileString, String fileName) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            OutputStream output = new FileOutputStream(fileString + "//" + fileName);

            byte[] data = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            input.close();
            output.close();

        } catch (IOException e) {
            System.out.println("Something went wrong creating " + fileName);
            e.printStackTrace();
        }
    }
}
