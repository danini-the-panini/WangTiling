/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wangtiling;

import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Daniel
 */
public class WangTiling extends JPanel
{

    public WangTiling()
    {
    }

    @Override
    public void paint(Graphics g)
    {
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Wang!");
        frame.add(new WangTiling());
        frame.setSize(500, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
