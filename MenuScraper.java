package com.annalisetarhan.menuscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.time.LocalDate;



public class MenuScraper {
    private static String millenniumMenu = "https://www.millenniumrestaurant.com/menu";
    private static String kindredHome = "https://barkindred.com";
    private static String graciasMadreMenu = "https://www.up2datemenu.com/plain_menu?menu_id=";
    private static int[] graciasMadreIDs = {9, 14, 18, 19, 34};
    private static String nativeFoodsPage = "https://www.nativefoods.com/our-menu";
    private static String veggieGrillPage = "https://www.veggiegrill.com/menu.html";

    public static void main(String[] args) {
        System.out.println("Creating this month's directory...\n");

        // Creates a directory based on current date, e.g. June2019
        // If directory couldn't be created, ends program early

        LocalDate date = LocalDate.now();
        String month = date.getMonth().toString().toLowerCase();
        month = Character.toUpperCase(month.charAt(0)) + month.substring(1);
        String dateString = month + "" + date.getYear();

        boolean created = false;
        try {
            File f = new File(dateString);
            created = f.mkdir();
        } catch (Exception e) {
            if (created) {
                System.out.println("This month's directory was created, but there was an error. Weird.");
                e.printStackTrace();
            } else {
                System.out.println("This month's directory was not created. Try again next month.");
                e.printStackTrace();
                return;
            }
        }

        // Saves each restaurant's menu in the new directory

        System.out.println("Fetching Millennium's menu...");
        saveMillenniumMenu(dateString);

        System.out.println("Fetching Kindred's menu...");
        saveKindredMenu(dateString);

        System.out.println("Fetching Gracias Madre's menu...");
        saveGraciasMadreMenu(dateString);

        System.out.println("Fetching Native Foods' menu...");
        saveNativeFoodsMenu(dateString);

        System.out.println("Fetching Veggie Grill's menu...");
        saveVeggieGrillMenu(dateString);

        System.out.println("\nThank you, come again!");
    }

    /*
     * Accesses Millennium's menu and uses Jsoup to save entire page as a txt file
     */

    private static void saveMillenniumMenu(String dateString) {
        String fileName = "MillenniumRaw.txt";
        Document doc;

        try {
            doc = Jsoup.connect(millenniumMenu).get();
        } catch (IOException e) {
            System.out.println("Millennium's menu could not be reached.");
            e.printStackTrace();
            return;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dateString + "//" + fileName));
            writer.write(doc.toString());
        } catch (IOException e) {
            System.out.println("Millennium's raw menu data could not be written.");
            e.printStackTrace();
        }

        cleanMillennium(doc, dateString);
    }

    /*
     * Extracts basic information from Millennium menu and saves as txt file
     */

    private static void cleanMillennium (Document doc, String dateString) {
        String fileName = dateString + "//" + "MillenniumClean.txt";

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

            boolean sectionIsRelevant = true;
            ArrayList<String> relevantSections = new ArrayList<>();
            relevantSections.add("Starters");
            relevantSections.add("Mains");
            relevantSections.add("Cocktails");
            relevantSections.add("DESSERTS");
            relevantSections.add("Sundays from 10:30am-2:00pm");

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

    private static void saveGraciasMadreMenu(String dateString) {
        String fileName = "GraciasMadre.txt";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dateString + "//" + fileName));

            // Menu is spread across multiple pages with urls differing only by final numerical id.
            // IDs stored in "graciasMadreIDs" array. Compiles all menus into a single txt file.

            for (int i : graciasMadreIDs) {
                String graciasMadreURL = graciasMadreMenu + i;
                Document doc = Jsoup.connect(graciasMadreURL).get();
                writer.write(doc.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Something went wrong with Gracias Madre's menu.");
            e.printStackTrace();
        }
    }

    /*
     * Accesses Kindred's menu and saves as a pdf
     */

    private static void saveKindredMenu(String dateString) {
        String fileName = "Kindred.pdf";
        URL kindredMenuURL = null;

        try {
            Document doc = Jsoup.connect(kindredHome).get();
            Elements links = doc.select("a[href]");

            // Depends on menu link having key "Menu"

            for (Element link : links) {
                if (link.text().equals("Menu")) {
                    kindredMenuURL = new URL(link.attr("abs:href"));
                }
            }

        } catch (IOException e) {
            System.out.println("Oh no! Kindred's menu wasn't where it was supposed to be");
            e.printStackTrace();
        }
        pdfMenuHelper(kindredMenuURL, dateString, fileName);
    }

    /*
     * Accesses Native Foods' menu and saves as pdf
     */

    private static void saveNativeFoodsMenu(String dateString) {
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
        }
        pdfMenuHelper(nativeFoodsMenuURL, dateString, fileName);
    }

    /*
     * Accesses Veggie Grill's menu and saves as a pdf
     */

    private static void saveVeggieGrillMenu(String dateString){
        String fileName = "VeggieGrill.pdf";
        URL veggieGrillMenuURL = null;

        try {
            Document doc = Jsoup.connect(veggieGrillPage).get();
            Elements links = doc.select("a[href]");

            // Depends on menu link with exact text: "Download Menu PDF"

            for (Element link : links) {
                if (link.text().equals("Download Menu PDF")) {
                    veggieGrillMenuURL = new URL(link.attr("abs:href"));
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Veggie Grill's menu wasn't there :(");
            e.printStackTrace();
        }
        pdfMenuHelper(veggieGrillMenuURL, dateString, fileName);
    }

    /*
     * Helper method for pdf menus
     */

    private static void pdfMenuHelper(URL url, String dateString, String fileName) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            OutputStream output = new FileOutputStream(dateString + "//" + fileName);

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
