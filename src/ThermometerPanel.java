import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Creates a visual thermometer. The thermometer has an animated temperature, which moves when the target changes.
 * The thermometer will automatically resize to fill the panel.
 * Stored units are in celsius, and are converted to fahrenheit for labeling.
 */
public class ThermometerPanel extends JPanel implements ActionListener {

    /* Graphics Fields */

    /**
     * Temperature limits for the labels.
     * Fahrenheit limits are decided by converting the celsius range and rounding it to the nearest ten,
     * so that the fahrenheit range is contained in the celsius range.
     */
    private final int MIN_CELSIUS, MAX_CELSIUS, MIN_FAHRENHEIT, MAX_FAHRENHEIT;

    /**
     * Body bounds is a dynamically calculated rectangle that defines the size of the body of the thermometer.
     * Other components are calculated in respect to body bounds.
     *
     * Mercury bounds define the location and size of the mercury of the thermometer.
     * The temperature is then calculated proportionally to the size of mercury bounds.
     */
    private Rectangle bodyBounds, mercuryBounds;

    /**
     * Body radius is the radius of the arc in the corners of the thermometer body.
     */
    private double bodyRadius;

    /**
     * Mercury radius is the radius of the mercury ball at the bottom of the thermometer.
     */
    private double mercuryRadius;

    /**
     * Defines the font size of the labels.
     * Changes in size with the rest of the thermometer.
     */
    private Font labelFont;

    /**
     * Spacing is the distance between labels for celsius and fahrenheit respectively.
     * It is calculated using the mercury bounds divied by the total number of multiples of ten
     * inside the min and max ranges for celsius and fahrenheit.
     */
    private double celsiusSpacing, fahrenheitSpacing;

    /**
     * These are the space before and after the fahrenheit labels.
     * Since fahrenheit and celsius do not match up these offset fahrenheit
     * so a more accurate measure can be determined.
     */
    private double fahrenheitLeadingOffset, fahrenheitTrailingOffset;

    /* Physics Fields */

    /**
     * Max speed defines the maximum value the temperature velocity can have.
     * Max force defined the maximum value the temperature acceleration can have.
     * Increasing either of these will cause the temperature to be more likely to over or under shoot target value.
     */
    private final double MAX_SPEED = 4.0, MAX_FORCE = 0.2;

    /**
     * Temperature is the current temperature of the thermometer.
     * Velocity is the rate at which the temperature is moving.
     * Acceleration increases or decreases the velocity.
     * Target is the temperature that the thermometer is seeking.
     * This allows the thermometer to have a delay in reaching the target
     * and cause the temperature to bounce before reaching the target.
     */
    private double temperature, velocity, acceleration, target;

    /* Animation Fields */

    /**
     * Timer controls the animation updates and temperature changes for the thermometer.
     */
    private Timer timer;

    /**
     * FPS, frames per second, controls the smoothness of the animation, if set too high
     * it will cause a large load on computer.
     */
    private final int FPS = 30;

    /**
     * Used to determine if sizes need to be recalculated.
     * Reduces the number of times calculate graphics needs to be called.
     */
    private Dimension lastSize;

    /**
     * Creates a thermometer panel based on a given range in celsius.
     *
     * @param min minimum value of thermometer in celsius.
     * @param max maximum value if thermometer in celsius.
     * @param t starting temperature value.
     */
    public ThermometerPanel(int min, int max, int t) {
        MIN_CELSIUS = min;
        MAX_CELSIUS = max;
        MIN_FAHRENHEIT = 10 * (int) Math.ceil(convertToFahrenheit(MIN_CELSIUS) / 10.0); // Round up to the nearest multiple of ten.
        MAX_FAHRENHEIT = 10 * (int) Math.floor(convertToFahrenheit(MAX_CELSIUS) / 10.0); // Round down to the nearest multiple of ten.

        // Initialize other fields
        temperature = t;
        velocity = 0;
        acceleration = 0;
        timer = new Timer(1000/FPS, this);
        lastSize = getSize();

        // Set a default size
        setPreferredSize(new Dimension(100, 400));
    }

    /**
     * Sets the target value of the thermometer.
     * The temperature will then try to move to the new target.
     * @param t the target.
     */
    public void seek(int t) {
        target = t;
        timer.start();
    }

    /**
     * Updates the physics and repaints the thermometer.
     */
    private void update() {
        // Check to see if temperature has arrived at the target
        if (Math.abs(velocity) < 0.1 && Math.abs(temperature - target) < 0.1) // Temperature may not hit exactly
            timer.stop(); // Stop animation
        else {
            double desired; // Velocity towards the target
            if (target - temperature > 15) // Dampening to reduce overshooting
                desired = (temperature < target ? 1 : -1) * MAX_SPEED; // Test for correct direction
            else
                desired = ((target - temperature) / 15) * MAX_SPEED; // Desired should decrease when approaching the target
            double steer = desired - velocity;
            acceleration += limit(steer, -MAX_FORCE, MAX_FORCE); // Apply steering force

            velocity += acceleration;
            velocity = limit(velocity, -MAX_SPEED, MAX_SPEED);
            temperature += velocity;
            acceleration = 0; // Reset acceleration

            repaint(); // Update graphics
        }
    }

    /**
     * Calculates the bounds of the thermometer.
     * This only needs to be called when the thermometer panel is resized.
     */
    private void calculateGraphics() {
        /*
         * Margin  : Proportional distance from the edge of the panel.
         * Ratio   : Width to height ratio.
         * Radius  : Curve radius of the corners of the thermometer.
         * Padding : Defines the a space between the body and the mercury.
         * Width   : Width of the mercury and defines the radius of the ball at the bottom.
         */
        final double MARGIN = 0.05, RATIO = 2.0/4.0, RADIUS = 0.2, PADDING = 0.1, WIDTH = 0.05;

        // Determine the maximum size of the thermometer
        bodyBounds = (getWidth() < getHeight() * RATIO) ? // Tests to see if the width or height restricts the size
                // Calculate relative to the width
                new Rectangle(
                        (int) (getWidth() * MARGIN),
                        (int) (getWidth() * MARGIN),
                        (int) (getWidth() - 2 * MARGIN * getWidth()),
                        (int) ((getWidth() - 2 * MARGIN * getWidth()) / RATIO)) :
                // Calculate relative to the height
                new Rectangle(
                        (int) (getHeight() * RATIO * MARGIN),
                        (int) (getHeight() * RATIO * MARGIN),
                        (int) (getHeight() * RATIO - 2 * MARGIN * getHeight() * RATIO),
                        (int) (getHeight() - 2 * MARGIN * getHeight() * RATIO));
        bodyRadius = (int) (bodyBounds.getWidth() * RADIUS);
        // Define mercury bounds using body bounds and not the size of panel
        mercuryBounds = new Rectangle(
                (int) (bodyBounds.getCenterX() - WIDTH/2),
                (int) (bodyBounds.getY() + bodyBounds.getHeight() * PADDING),
                (int) (bodyBounds.getWidth() * WIDTH),
                (int) (bodyBounds.getHeight() * (1 - 2 * PADDING)));
        mercuryRadius = (int) (bodyBounds.getWidth() * 2 * WIDTH);

        // Determine font size based on body width
        labelFont = new Font("Sans-Serif", Font.PLAIN, (int) (12 * (bodyBounds.getWidth() / 200.0)));

        /* Spacing = Height / Number of labels
        Number of labels = Range / 10; */
        celsiusSpacing = mercuryBounds.getHeight() / ((MAX_CELSIUS - MIN_CELSIUS) / 10.0);

        // Leading offset finds the amount rounded off the max celsius when converted
        fahrenheitLeadingOffset = map(MAX_CELSIUS - convertToCelsius(MAX_FAHRENHEIT), 0, 10, 0, celsiusSpacing);
        // Trailing offset finds the amount rounded off the min celsius when converted
        fahrenheitTrailingOffset = map(convertToCelsius(MIN_FAHRENHEIT) - MIN_CELSIUS, 0, 10, 0, celsiusSpacing);

        /* Spacing = (Height - Offsets) / Number of labels
        Number of labels = Range / 10; */
        fahrenheitSpacing = (mercuryBounds.getHeight() - (fahrenheitLeadingOffset + fahrenheitTrailingOffset)) / ((MAX_FAHRENHEIT - MIN_FAHRENHEIT) / 10.0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Calculate graphics only needs to be called when there is a size change
        Dimension currentSize = getSize();
        if (lastSize == null || !currentSize.equals(lastSize)) { // Check for null and size change
            lastSize = currentSize; // Update last size
            calculateGraphics();
        }

        // Draw the thermometer body
        g.setColor(Color.WHITE);
        g.fillRoundRect(
                (int) bodyBounds.getX(),
                (int) bodyBounds.getY(),
                (int) bodyBounds.getWidth(),
                (int) bodyBounds.getHeight(),
                (int) bodyRadius, (int) bodyRadius);

        // Draw mercury background
        g.setColor(new Color(0xEEEEEE));
        g.fillRect(
                (int) mercuryBounds.getX(),
                (int) mercuryBounds.getY(),
                (int) mercuryBounds.getWidth(),
                (int) mercuryBounds.getHeight());

        // Draw the mercury
        g.setColor(new Color(0x8A0707));
        double mercuryHeight = (limit(temperature, MIN_CELSIUS, MAX_CELSIUS) - MIN_CELSIUS) / (double) (MAX_CELSIUS - MIN_CELSIUS);
        g.fillRect(
                (int) mercuryBounds.getX(),
                (int) (mercuryBounds.getY() + (mercuryBounds.getHeight() * (1 - mercuryHeight))),
                (int) mercuryBounds.getWidth(),
                (int) (mercuryBounds.getHeight() * mercuryHeight));
        g.fillOval(
                (int) (bodyBounds.getCenterX() - mercuryRadius/4.0),
                (int) (mercuryBounds.getY() + mercuryBounds.getHeight() - mercuryRadius/2.0),
                (int) mercuryRadius, (int) mercuryRadius);


        // Draw unit labels
        g.setColor(Color.BLACK);
        g.setFont(labelFont);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString("C",
                (int) (bodyBounds.getX() + bodyRadius / 2.0),
                (int) (bodyBounds.getY() + metrics.getHeight() / 2.0 + bodyRadius / 2.0));
        g.drawString("F",
                (int) (bodyBounds.getX() + bodyBounds.getWidth() - metrics.stringWidth("F") - bodyRadius / 2.0),
                (int) (bodyBounds.getY() + metrics.getHeight() / 2.0 + bodyRadius / 2.0));

        // Find distance from mercury so labels do not overlap
        int celsiusTextOffset = Math.max(metrics.stringWidth(Integer.toString(MAX_CELSIUS)), metrics.stringWidth(Integer.toString(MIN_CELSIUS)));
        // Draw labels
        for (int i = MAX_CELSIUS, n = 0; i >= MIN_CELSIUS; i -= 10, n++) {
            g.drawString(Integer.toString(i),
                    (int) (mercuryBounds.getCenterX() - mercuryBounds.getWidth() - celsiusTextOffset),
                    (int) (mercuryBounds.getY() + n * celsiusSpacing + metrics.getHeight()/2.0));
        }
        for (int i = MAX_FAHRENHEIT, n = 0; i >= MIN_FAHRENHEIT; i -= 10, n++) {
            g.drawString(Integer.toString(i),
                    (int) (mercuryBounds.getCenterX() + mercuryBounds.getWidth()),
                    (int) (mercuryBounds.getY() + n * fahrenheitSpacing + fahrenheitLeadingOffset + metrics.getHeight()/2.0));
        }
    }

    /**
     * Implemented method for the animation timer.
     * @param e event.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        update();
    }

    /* Math Functions */

    /**
     * Limits a value to a set range.
     *
     * @param value value to limit.
     * @param min lower limit of range.
     * @param max upper limit of range.
     * @return the limited value.
     */
    private static double limit(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * Maps the value from the first range to the second range.
     *
     * @param value value to be converted.
     * @param start1 lower limit of the first range.
     * @param stop1 upper limit of the first range.
     * @param start2 lower limit of the second range.
     * @param stop2 upper limit of the second range.
     * @return the mapped value.
     */
    private static double map(double value, double start1, double stop1, double start2, double stop2) {
        return (value - start1) / (stop1 - start1) * (stop2 - start2) + start2;
    }

    /**
     * Converts celsius to fahrenheit.
     *
     * @param c celsius to be converted.
     * @return the fahrenheit equivalent.
     */
    private static double convertToFahrenheit(int c) {
        return 1.8 * c + 32;
    }

    /**
     * Converts fahrenheit to celsius.
     *
     * @param f fahrenheit to be converted.
     * @return the celsius equivalent.
     */
    private static double convertToCelsius(int f) {
        return 5.0 * (f - 32) / 9.0;
    }

}