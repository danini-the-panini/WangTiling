/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wangtiling;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author Daniel
 */
public class WangTiling extends JPanel
{
    BufferedImage test;
    BufferedImage seams;
    
    int w, h;
    
    long seed;
    
    int[][] f = {
        { 0, 1,12,13},
        { 2, 3,13,14},
        { 4, 5, 8, 9},
        { 6, 7,10,11}
    };
    
    int[] g = {0,0,1,1,2,2,3,3,2,2,3,3,0,0,1,1};
    
    int[][] border = {
        {0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0},
        {1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0},
        {0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1},
        {0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0}
    };
    
    int[] z = {12,13,15,14, 0, 1, 3, 2, 8, 9,11,10, 4, 5, 7, 6};
    
    int[][] dir = {
        {1,0},
        {-1,0},
        {0,1},
        {0,-1}
    };
    
    int[][] tiles;
    
    Random random;

    public WangTiling()
    {
        try
        {
            test = ImageIO.read(new File("test.png"));
            w = test.getWidth()/4;
            h = test.getHeight()/4;
            seams = ImageIO.read(new File("seams.png"));
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void paint(Graphics g)
    {
        random = new Random(seed);
        tiles = new int[getHeight()/h+1][getWidth()/w+1];
        for (int i = 0; i < tiles.length; i++)
            for (int j = 0; j < tiles[i].length; j++)
                tiles[i][j] = -1;
        for (int i = 0; i < tiles.length; i++)
        {
            for (int j = 0; j < tiles[i].length; j++)
            {
                int x = get(i,j);
                tiles[i][j] = x;
                int[] p = p(x);
                g.drawImage(seams, j*w, i*h, j*w+w, i*h+h, p[0]*w, p[1]*h, p[0]*w+w, p[1]*h+h, this);
            }
        }
    }
    
    public int[] p(int x)
    {
        return new int[]{x % 4, x / 4};
    }
    
    public int get(int i, int j)
    {
        int z = 0;
        for (int d = 0; d < 4; d++)
            z |= g(i,j,d) << (3-d);
        return this.z[z];
    }
    
    public int wrap(int x, int y)
    {
        if (x >= y) return x % y;
        if (x < 0) return x + y;
        return x;
    }
    
    public int get(int i, int j, int[] dir)
    {
        return tiles[wrap(i+dir[0],tiles.length)][wrap(j+dir[1],tiles[0].length)];
    }
    
    public int g(int i, int j, int d)
    {
        int x = get(i,j,dir[d]);
        if (x < 0) return p();
        return border[d][x];
    }
    
    public int p()
    {
        return random.nextBoolean() ? 1 : 0;
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
