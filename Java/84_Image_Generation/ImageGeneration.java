import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/**
 * 84 - Generating an image: draw a bar chart into a BufferedImage, write it as a
 * PNG, read it back and inspect the pixels. No display is required.
 *
 * Compile and run:
 *   javac ImageGeneration.java
 *   java ImageGeneration
 */
public class ImageGeneration {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final Color PAPER = new Color(0xFF, 0xFF, 0xFF);
    static final Color AXIS = Color.BLACK;
    static final Color[] BAR_COLOURS = {
            new Color(0x33, 0x66, 0xCC),
            new Color(0xCC, 0x33, 0x33),
            new Color(0x33, 0x99, 0x33),
    };

    static BufferedImage drawChart(int[] values, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            // Sharp edges make the pixel assertions below exact.
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setColor(PAPER);
            graphics.fillRect(0, 0, width, height);

            int largest = Arrays.stream(values).max().orElse(1);
            int barWidth = width / (values.length * 2);
            int gap = barWidth;
            int baseline = height - 20;
            int usableHeight = baseline - 20;

            for (int i = 0; i < values.length; i++) {
                int barHeight = (int) Math.round((double) values[i] / largest * usableHeight);
                int x = gap + i * (barWidth + gap);
                int y = baseline - barHeight;
                graphics.setColor(BAR_COLOURS[i % BAR_COLOURS.length]);
                graphics.fillRect(x, y, barWidth, barHeight);
            }

            graphics.setColor(AXIS);
            graphics.drawLine(0, baseline, width, baseline);
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 9));
            graphics.drawString("values " + Arrays.toString(values), 4, height - 4);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Convert by drawing through a grey colour model. */
    static BufferedImage toGreyscale(BufferedImage source) {
        BufferedImage grey = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grey.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return grey;
    }

    static Map<Integer, Integer> histogram(BufferedImage image) {
        Map<Integer, Integer> counts = new TreeMap<>();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                counts.merge(image.getRGB(x, y), 1, Integer::sum);
            }
        }
        return counts;
    }

    public static void main(String[] args) throws Exception {
        // No display needed: set this before any AWT class initialises.
        System.setProperty("java.awt.headless", "true");

        int width = 200;
        int height = 120;
        int[] values = {10, 20, 5};
        BufferedImage chart = drawChart(values, width, height);

        check(chart.getWidth() == width && chart.getHeight() == height, "the canvas is 200 by 120");
        check(chart.getType() == BufferedImage.TYPE_INT_RGB, "the colour model is plain RGB");
        System.out.println("canvas      : " + chart.getWidth() + "x" + chart.getHeight());

        // ---- exact pixel expectations -----------------------------------------
        // barWidth = 200/6 = 33, gap = 33, baseline = 100, usable height = 80.
        // Bar 0: x 33..65, height round(10/20*80) = 40, so y 60..99.
        // Bar 1: x 99..131, height 80, so y 20..99.
        // Bar 2: x 165..197, height 20, so y 80..99.
        check(chart.getRGB(40, 70) == BAR_COLOURS[0].getRGB(), "the first bar is blue where it should be");
        check(chart.getRGB(110, 50) == BAR_COLOURS[1].getRGB(), "the tallest bar is red");
        check(chart.getRGB(170, 85) == BAR_COLOURS[2].getRGB(), "the shortest bar is green");
        check(chart.getRGB(40, 50) == PAPER.getRGB(), "just above the first bar is still paper");
        check(chart.getRGB(1, 1) == PAPER.getRGB(), "the top left corner is blank");
        check(chart.getRGB(width - 1, 100) == AXIS.getRGB(), "the axis line is drawn along the baseline");
        System.out.println("pixels      : all three bars and the axis are where the maths says");

        Map<Integer, Integer> counts = histogram(chart);
        check(counts.get(PAPER.getRGB()) > width * height / 2, "paper is the dominant colour");
        check(counts.containsKey(BAR_COLOURS[0].getRGB())
                && counts.containsKey(BAR_COLOURS[1].getRGB())
                && counts.containsKey(BAR_COLOURS[2].getRGB()), "every bar colour is present");
        check(counts.get(BAR_COLOURS[1].getRGB()) > counts.get(BAR_COLOURS[2].getRGB()),
                "the tall bar covers more pixels than the short one");
        System.out.println("histogram   : " + counts.size() + " distinct colours, "
                + counts.get(PAPER.getRGB()) + " of them paper");

        Path directory = Files.createTempDirectory("java-image-");
        try {
            // ---- write and read back ------------------------------------------
            Path png = directory.resolve("chart.png");
            check(ImageIO.write(chart, "png", png.toFile()), "a PNG writer is available");
            check(Files.size(png) > 0, "the file has bytes");
            check(Files.size(png) < width * height * 4L,
                    "and it is smaller than the raw pixels would be: " + Files.size(png) + " bytes");

            BufferedImage loaded = ImageIO.read(png.toFile());
            check(loaded != null, "the PNG reads back");
            check(loaded.getWidth() == width && loaded.getHeight() == height, "the size survives");

            boolean identical = true;
            for (int y = 0; y < height && identical; y++) {
                for (int x = 0; x < width; x++) {
                    if (chart.getRGB(x, y) != loaded.getRGB(x, y)) {
                        identical = false;
                        break;
                    }
                }
            }
            check(identical, "PNG is lossless, so every pixel is identical");
            System.out.printf("png         : %s, %d bytes, %d pixels identical after the round trip%n",
                    png.getFileName(), Files.size(png), width * height);

            // ---- greyscale -----------------------------------------------------
            BufferedImage grey = toGreyscale(chart);
            check(grey.getType() == BufferedImage.TYPE_BYTE_GRAY, "the converted image is byte grey");
            int greyPixel = grey.getRGB(40, 70);
            int red = (greyPixel >> 16) & 0xFF;
            int green = (greyPixel >> 8) & 0xFF;
            int blue = greyPixel & 0xFF;
            check(red == green && green == blue, "a grey pixel has equal components");
            check(red > 0 && red < 255, "and the blue bar became a mid grey, not pure black or white");
            System.out.println("greyscale   : the blue bar became grey level " + red);

            Path greyFile = directory.resolve("chart-grey.png");
            ImageIO.write(grey, "png", greyFile.toFile());
            BufferedImage greyReloaded = ImageIO.read(greyFile.toFile());
            int reloadedLevel = (greyReloaded.getRGB(40, 70) >> 16) & 0xFF;
            check(Math.abs(reloadedLevel - red) <= 1,
                    "the greyscale image round trips: " + reloadedLevel + " against " + red);

            // ---- format support -------------------------------------------------
            String[] formats = ImageIO.getWriterFormatNames();
            check(Arrays.asList(formats).contains("png"), "PNG is registered as a writer");
            check(Arrays.stream(formats).anyMatch(name -> name.equalsIgnoreCase("jpg")
                    || name.equalsIgnoreCase("jpeg")), "and so is JPEG");
            check(!ImageIO.write(chart, "no-such-format", directory.resolve("x.bin").toFile()),
                    "an unknown format reports that it could not write");

            // ---- edge cases -----------------------------------------------------
            BufferedImage single = drawChart(new int[] {7}, 40, 40);
            check(single.getWidth() == 40 && single.getHeight() == 40, "a one bar chart still renders");
            BufferedImage flat = drawChart(new int[] {0, 0}, 40, 40);
            check(flat.getRGB(0, 0) == PAPER.getRGB(), "all zero values draw nothing but paper");

            // the files are small enough to be worth listing
            try (var paths = Files.walk(directory)) {
                long files = paths.filter(Files::isRegularFile).count();
                check(files == 2, "two images were written");
            }
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("All checks passed.");
    }
}
