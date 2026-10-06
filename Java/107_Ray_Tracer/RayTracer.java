import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

/**
 * 107 - A ray tracer: spheres and a ground plane, lit by point lights with
 * shadows, rendered by shooting one ray per sample through a camera and writing
 * the result as a PNG.
 *
 * Rendering is a pure function of the scene, so two runs produce identical
 * pixels - which makes the whole thing testable.
 *
 * Compile and run:
 *   javac RayTracer.java
 *   java RayTracer
 */
public class RayTracer {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------------ maths
    record Vec3(double x, double y, double z) {
        static final Vec3 ZERO = new Vec3(0, 0, 0);
        static final Vec3 WHITE = new Vec3(1, 1, 1);

        Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        Vec3 subtract(Vec3 other) {
            return new Vec3(x - other.x, y - other.y, z - other.z);
        }

        Vec3 scale(double factor) {
            return new Vec3(x * factor, y * factor, z * factor);
        }

        /** Component by component, which is what colours need. */
        Vec3 modulate(Vec3 other) {
            return new Vec3(x * other.x, y * other.y, z * other.z);
        }

        double dot(Vec3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        double length() {
            return Math.sqrt(dot(this));
        }

        Vec3 normalise() {
            double magnitude = length();
            return magnitude == 0 ? ZERO : scale(1 / magnitude);
        }

        @Override
        public String toString() {
            return String.format("(%.3f, %.3f, %.3f)", x, y, z);
        }
    }

    record Ray(Vec3 origin, Vec3 direction) {
        Vec3 at(double distance) {
            return origin.add(direction.scale(distance));
        }
    }

    // --------------------------------------------------------------- surfaces
    sealed interface Surface permits Sphere, Plane {
        /** Distance along the ray, or -1 when it misses or falls outside the range. */
        double hit(Ray ray, double minimum, double maximum);

        Vec3 normalAt(Vec3 point);

        Vec3 colour();
    }

    record Sphere(Vec3 centre, double radius, Vec3 colour) implements Surface {
        @Override
        public double hit(Ray ray, double minimum, double maximum) {
            Vec3 toCentre = ray.origin().subtract(centre);
            double a = ray.direction().dot(ray.direction());
            double b = 2 * toCentre.dot(ray.direction());
            double c = toCentre.dot(toCentre) - radius * radius;
            double discriminant = b * b - 4 * a * c;
            if (discriminant < 0) {
                return -1;                       // the ray misses the sphere entirely
            }
            double root = Math.sqrt(discriminant);
            double near = (-b - root) / (2 * a);
            if (near > minimum && near < maximum) {
                return near;
            }
            double far = (-b + root) / (2 * a);
            return far > minimum && far < maximum ? far : -1;
        }

        @Override
        public Vec3 normalAt(Vec3 point) {
            return point.subtract(centre).normalise();
        }
    }

    /** An infinite ground plane at a fixed height. */
    record Plane(double height, Vec3 colour) implements Surface {
        @Override
        public double hit(Ray ray, double minimum, double maximum) {
            if (ray.direction().y() == 0) {
                return -1;                        // parallel to the plane
            }
            double distance = (height - ray.origin().y()) / ray.direction().y();
            return distance > minimum && distance < maximum ? distance : -1;
        }

        @Override
        public Vec3 normalAt(Vec3 point) {
            return new Vec3(0, 1, 0);
        }
    }

    // ------------------------------------------------------------------ scene
    record Light(Vec3 position, Vec3 colour) {
    }

    record Camera(Vec3 origin, double fieldOfViewDegrees) {
    }

    record Scene(Camera camera, List<Surface> surfaces, List<Light> lights,
                 Vec3 background, Vec3 ambient) {
    }

    /** The scene used throughout: a red sphere over a grey floor, in a blue sky. */
    static Scene defaultScene() {
        return new Scene(
                new Camera(Vec3.ZERO, 60),
                List.of(
                        new Sphere(new Vec3(0, 0, -3), 1.0, new Vec3(0.9, 0.2, 0.2)),
                        new Plane(-1.0, new Vec3(0.4, 0.4, 0.45))),
                List.of(new Light(new Vec3(-3, 4, 2), Vec3.WHITE)),
                new Vec3(0.5, 0.7, 1.0),          // sky
                new Vec3(0.15, 0.15, 0.15));      // ambient light
    }

    // ---------------------------------------------------------------- tracing
    /** Follows one ray to the nearest surface and returns its colour. */
    static Vec3 trace(Scene scene, Ray ray, boolean shadows) {
        double closest = Double.MAX_VALUE;
        Surface hitSurface = null;

        for (Surface surface : scene.surfaces()) {
            double distance = surface.hit(ray, 0.001, closest);
            if (distance > 0) {
                closest = distance;
                hitSurface = surface;
            }
        }
        if (hitSurface == null) {
            return scene.background();
        }

        Vec3 point = ray.at(closest);
        Vec3 normal = hitSurface.normalAt(point);
        Vec3 colour = scene.ambient().modulate(hitSurface.colour());

        for (Light light : scene.lights()) {
            Vec3 toLight = light.position().subtract(point);
            double distanceToLight = toLight.length();
            Vec3 direction = toLight.normalise();

            if (shadows && isShadowed(scene, point, direction, distanceToLight)) {
                continue;                     // this light is blocked
            }

            double diffuse = Math.max(0, normal.dot(direction));
            Vec3 brightness = new Vec3(diffuse, diffuse, diffuse);
            colour = colour.add(hitSurface.colour().modulate(brightness).modulate(light.colour()));
        }
        return colour;
    }

    static boolean isShadowed(Scene scene, Vec3 point, Vec3 direction, double distanceToLight) {
        Ray shadowRay = new Ray(point, direction);
        for (Surface surface : scene.surfaces()) {
            if (surface.hit(shadowRay, 0.001, distanceToLight) > 0) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------- rendering
    static BufferedImage render(Scene scene, int width, int height,
                                boolean shadows, int samplesPerAxis) {
        if (samplesPerAxis < 1) {
            throw new IllegalArgumentException("samplesPerAxis must be at least 1");
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double halfHeight = Math.tan(Math.toRadians(scene.camera().fieldOfViewDegrees() / 2));
        double halfWidth = halfHeight * ((double) width / height);
        double samples = samplesPerAxis * samplesPerAxis;

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                Vec3 total = Vec3.ZERO;
                for (int sampleY = 0; sampleY < samplesPerAxis; sampleY++) {
                    for (int sampleX = 0; sampleX < samplesPerAxis; sampleX++) {
                        double across = ((column + (sampleX + 0.5) / samplesPerAxis) / width) * 2 - 1;
                        double up = 1 - ((row + (sampleY + 0.5) / samplesPerAxis) / height) * 2;
                        Vec3 direction = new Vec3(across * halfWidth, up * halfHeight, -1).normalise();
                        total = total.add(trace(scene, new Ray(scene.camera().origin(), direction), shadows));
                    }
                }
                image.setRGB(column, row, toRgb(total.scale(1 / samples)));
            }
        }
        return image;
    }

    static int clampChannel(double value) {
        return (int) Math.round(Math.min(1, Math.max(0, value)) * 255);
    }

    static int toRgb(Vec3 colour) {
        return (clampChannel(colour.x()) << 16) | (clampChannel(colour.y()) << 8) | clampChannel(colour.z());
    }

    static int red(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    static int green(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    static int blue(int rgb) {
        return rgb & 0xFF;
    }

    static int brightness(int rgb) {
        return (red(rgb) + green(rgb) + blue(rgb)) / 3;
    }

    static int distinctColours(BufferedImage image) {
        Set<Integer> colours = new HashSet<>();
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                colours.add(image.getRGB(column, row) & 0xFFFFFF);
            }
        }
        return colours.size();
    }

    static int countDarkerThan(BufferedImage image, int threshold) {
        int count = 0;
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                if (brightness(image.getRGB(column, row)) < threshold) {
                    count++;
                }
            }
        }
        return count;
    }

    static boolean identical(BufferedImage left, BufferedImage right) {
        if (left.getWidth() != right.getWidth() || left.getHeight() != right.getHeight()) {
            return false;
        }
        for (int row = 0; row < left.getHeight(); row++) {
            for (int column = 0; column < left.getWidth(); column++) {
                if (left.getRGB(column, row) != right.getRGB(column, row)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("java.awt.headless", "true");

        Scene scene = defaultScene();
        int width = 160;
        int height = 100;
        BufferedImage image = render(scene, width, height, true, 1);

        // ---- the geometry of the picture -------------------------------------
        check(image.getWidth() == width && image.getHeight() == height, "the canvas is the size asked for");
        check(image.getType() == BufferedImage.TYPE_INT_RGB, "and plain RGB");

        int background = toRgb(scene.background());
        // getRGB hands back ARGB, so the alpha byte has to be masked off.
        check((image.getRGB(0, 0) & 0xFFFFFF) == background, "the top left corner is sky: nothing is up there");
        check((image.getRGB(width - 1, 0) & 0xFFFFFF) == background, "and so is the top right");

        int centre = image.getRGB(width / 2, height / 2);
        check(red(centre) > green(centre) && red(centre) > blue(centre),
                "the middle of the picture is the red sphere: " + Integer.toHexString(centre));
        check(red(centre) > 150, "and it is brightly lit, not in shadow: red=" + red(centre));
        System.out.printf("centre       : #%06x (r=%d g=%d b=%d)%n",
                centre & 0xFFFFFF, red(centre), green(centre), blue(centre));

        int floor = image.getRGB(2, height - 2) & 0xFFFFFF;
        check(floor != background, "the bottom left is the floor, not the sky");
        check(brightness(floor) > 20, "the floor is lit: brightness " + brightness(floor));
        check(Math.abs(red(floor) - green(floor)) < 30, "and it is grey rather than red");
        System.out.printf("floor        : #%06x (brightness %d)%n", floor & 0xFFFFFF, brightness(floor));

        // ---- shadows change the picture --------------------------------------
        BufferedImage withShadows = render(scene, width, height, true, 1);
        BufferedImage withoutShadows = render(scene, width, height, false, 1);
        check(!identical(withShadows, withoutShadows),
                "turning shadows off changes the image, so shadowing really happens");

        int darkWith = countDarkerThan(withShadows, 60);
        int darkWithout = countDarkerThan(withoutShadows, 60);
        check(darkWith > darkWithout,
                "the shadowed render has more dark pixels: " + darkWith + " against " + darkWithout);

        // find a pixel that the shadow darkens, and show both values
        int shadowColumn = -1;
        int shadowRow = -1;
        outer:
        for (int row = height / 2; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if (brightness(withShadows.getRGB(column, row)) < brightness(withoutShadows.getRGB(column, row)) - 20) {
                    shadowColumn = column;
                    shadowRow = row;
                    break outer;
                }
            }
        }
        check(shadowColumn >= 0, "at least one floor pixel is notably darker with shadows on");
        System.out.printf("shadow       : pixel (%d,%d) is %d bright with shadows, %d without%n",
                shadowColumn, shadowRow,
                brightness(withShadows.getRGB(shadowColumn, shadowRow)),
                brightness(withoutShadows.getRGB(shadowColumn, shadowRow)));

        // ---- antialiasing -----------------------------------------------------
        BufferedImage antialiased = render(scene, width, height, true, 2);
        check(!identical(antialiased, withShadows),
                "two samples per pixel changes the silhouette");
        check(distinctColours(antialiased) >= distinctColours(withShadows),
                "smoothing does not reduce the number of colours: "
                        + distinctColours(antialiased) + " against " + distinctColours(withShadows));
        System.out.printf("antialiasing : %d colours at one sample, %d at four%n",
                distinctColours(withShadows), distinctColours(antialiased));

        // ---- rendering is pure ------------------------------------------------
        check(identical(render(scene, width, height, true, 1), withShadows),
                "the same scene renders to identical pixels every time");
        System.out.println("determinism  : two renders are pixel for pixel identical");

        // ---- an empty scene is all sky ----------------------------------------
        Scene empty = new Scene(scene.camera(), List.of(), scene.lights(),
                scene.background(), scene.ambient());
        BufferedImage sky = render(empty, 20, 20, true, 1);
        check(distinctColours(sky) == 1, "with no surfaces there is only the background colour");
        check((sky.getRGB(10, 10) & 0xFFFFFF) == background, "and every pixel is that colour");
        System.out.println("empty scene  : one colour, the background");

        // ---- a ray that misses everything -------------------------------------
        Vec3 away = trace(scene, new Ray(new Vec3(0, 50, 0), new Vec3(0, 1, 0).normalise()), true);
        check(toRgb(away) == background, "a ray pointing at the sky returns the background");
        System.out.println("miss         : a ray into the sky gives " + away);

        // ---- the geometry helpers ---------------------------------------------
        check(new Vec3(3, 4, 0).length() == 5, "3-4-5 triangle");
        check(new Vec3(0, 0, 5).normalise().equals(new Vec3(0, 0, 1)), "normalising gives unit length");
        check(Math.abs(new Vec3(1, 1, 1).normalise().length() - 1) < 1e-12, "and the result is unit length");
        check(Vec3.ZERO.normalise().equals(Vec3.ZERO), "the zero vector does not divide by zero");
        check(new Vec3(1, 2, 3).dot(new Vec3(4, 5, 6)) == 32, "the dot product");
        check(new Vec3(1, 2, 3).modulate(new Vec3(2, 2, 2)).equals(new Vec3(2, 4, 6)), "component wise colours");
        check(new Vec3(1, 2, 3).subtract(new Vec3(1, 1, 1)).equals(new Vec3(0, 1, 2)), "subtraction");
        System.out.println("vector maths : length, normalise, dot and component wise multiply");

        // ---- intersection in isolation ----------------------------------------
        Sphere sphere = new Sphere(new Vec3(0, 0, -5), 1, Vec3.WHITE);
        Ray straightAt = new Ray(Vec3.ZERO, new Vec3(0, 0, -1));
        check(Math.abs(sphere.hit(straightAt, 0.001, Double.MAX_VALUE) - 4) < 1e-9,
                "a ray down the axis hits the sphere at distance 4");
        check(sphere.hit(new Ray(Vec3.ZERO, new Vec3(0, 1, 0)), 0.001, Double.MAX_VALUE) == -1,
                "a ray upwards misses it");
        // a ray that would hit behind the origin is not reported
        check(sphere.hit(new Ray(new Vec3(0, 0, 0), new Vec3(0, 0, 1)), 0.001, Double.MAX_VALUE) == -1,
                "surfaces behind the ray are ignored");
        check(sphere.normalAt(new Vec3(0, 1, -5)).equals(new Vec3(0, 1, 0)),
                "the normal at the top of the sphere points up");

        Plane ground = new Plane(-1, Vec3.WHITE);
        check(Math.abs(ground.hit(new Ray(Vec3.ZERO, new Vec3(0, -1, 0)), 0.001, Double.MAX_VALUE) - 1) < 1e-9,
                "a downward ray hits the floor one unit away");
        check(ground.hit(new Ray(Vec3.ZERO, new Vec3(1, 0, 0)), 0.001, Double.MAX_VALUE) == -1,
                "a horizontal ray never meets it");
        System.out.println("intersection : sphere roots and the floor plane behave");

        // ---- clamping ----------------------------------------------------------
        check(clampChannel(-1.0) == 0 && clampChannel(2.0) == 255, "brightness is clamped to a byte");
        check(clampChannel(0.5) == 128, "and rounded");
        System.out.println("clamping     : out of range colours are pinned to 0 and 255");

        // ---- writing the image ------------------------------------------------
        Path directory = Files.createTempDirectory("java-raytracer-");
        try {
            Path png = directory.resolve("render.png");
            check(ImageIO.write(withShadows, "png", png.toFile()), "PNG is writable");
            check(Files.size(png) > 0, "and the file has bytes");

            BufferedImage reloaded = ImageIO.read(png.toFile());
            check(reloaded != null, "the file reads back");
            check(identical(reloaded, withShadows), "PNG is lossless, so every pixel survives");
            check(Files.size(png) < width * height * 3L,
                    "and the file is smaller than the raw pixels: " + Files.size(png) + " bytes");

            // a bigger render is slower but the same code path
            Path bigger = directory.resolve("bigger.png");
            ImageIO.write(render(scene, 320, 200, true, 2), "png", bigger.toFile());
            check(Files.size(bigger) > Files.size(png), "the larger antialiased render is a bigger file");
            System.out.printf("output       : %s %d bytes, %s %d bytes%n",
                    png.getFileName(), Files.size(png), bigger.getFileName(), Files.size(bigger));
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }

        // ---- bad input ---------------------------------------------------------
        try {
            render(scene, 10, 10, true, 0);
            throw new AssertionError("zero samples per pixel should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad samples  : " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }
}
