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
    
    int[][] border = {
        {1,1,1,1,1,1,1,1,0,0,0,0,0,0,0,0},
        {0,0,0,0,1,1,1,1,1,1,1,1,0,0,0,0},
        {0,1,1,0,0,1,1,0,0,1,1,0,0,1,1,0},
        {0,0,1,1,0,0,1,1,0,0,1,1,0,0,1,1}
    };
    
    int[] f = {12,13,15,14, 0, 1, 3, 2, 8, 9,11,10, 4, 5, 7, 6};
    
    int up = 0;
    int dn = 1;
    int lt = 2;
    int rt = 3;
    
    int[][] dir = {
        {-1,0},
        {1,0},
        {0,-1},
        {0,1}
    };
    
    int[] o = {
        1,0,3,2
    };
    
    int[][] tiles;
    
    Random random;

    public WangTiling(String tex)
    {
        seed = System.currentTimeMillis();
        try
        {
            test = ImageIO.read(new File(tex));
            w = test.getWidth()/4;
            h = test.getHeight()/4;
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
                int x = calc(i,j);
                tiles[i][j] = x;
                int[] p = pnt(x);
                g.drawImage(test, j*w, i*h, j*w+w, i*h+h, p[0]*w, p[1]*h, p[0]*w+w, p[1]*h+h, this);
                g.drawRect(j*w, i*h, w, h);
            }
        }
    }
    
    public int[] pnt(int x)
    {
        return new int[]{x % 4, x / 4};
    }
    
    public int calc(int i, int j)
    {
        int z;
        do
        {
            z = 0;
            for (int d = 0; d < 4; d++)
            {
                z |= g(i,j,d) << (3-d);
            }
        }
        while (bad(i,j,f[z]));
        return f[z];
    }
    
    public boolean bad(int i, int j, int x)
    {
        for (int d = 0; d < 4; d++)
        {
            if (get(i,j,dir[d]) == x) return true;
            if (get(i,j,add(dir[d],dir[(d+2)%4])) == x) return true;
        }
        return false;
    }
    
    public int[] add(int[] a, int[] b)
    {
        int[] c = new int[a.length];
        for (int i = 0; i < a.length; i++)
            c[i] = a[i] + b[i];
        return c;
    }
    
    public int wrap(int a, int b)
    {
        if (a >= b) return a % b;
        if (a < 0) return a + b;
        return a;
    }
    
    public int get(int i, int j, int[] dir)
    {
        try
        {
            return tiles[i+dir[0]][j+dir[1]];
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            return -1;
        }
    }
    
    public int g(int i, int j, int d)
    {
        int x = get(i,j,dir[d]);
        if (x < 0) return p();
        return border[d][x];
    }
    
    public int p()
    {
        return random.nextInt(2);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Wang!");
        frame.add(new WangTiling(args.length > 0 ? args[0] : "test.png"));
        frame.setSize(500, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
