package wangtiling;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    Image tex;
    int tileW, tileH;
    long seed;
    int[][] borders =
    {
        {
            1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0
        },
        {
            0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0
        },
        {
            0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0
        },
        {
            0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1
        }
    };
    int[] hash =
    {
        12, 13, 15, 14, 0, 1, 3, 2, 8, 9, 11, 10, 4, 5, 7, 6
    };
    int UP = 0;
    int DOWN = 1;
    int LEFT = 2;
    int RIGHT = 3;
    int[][] directions =
    {
        {
            -1, 0
        },
        {
            1, 0
        },
        {
            0, -1
        },
        {
            0, 1
        }
    };
    int[][] diag =
    {
        {
            -1, -1
        },
        {
            -1, 1
        },
        {
            1, 1
        },
        {
            1, -1
        }
    };
    int[][] tiles;
    Random random;

    public WangTiling(String fileName)
    {
        seed = System.currentTimeMillis();
        try
        {
            this.tex = ImageIO.read(new File(fileName));
            tileW = this.tex.getWidth(this) / 4;
            tileH = this.tex.getHeight(this) / 4;
        } catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                seed = System.currentTimeMillis();
                repaint();
            }
        });
    }

    @Override
    public void paint(Graphics g)
    {
        random = new Random(seed);
        tiles = new int[getHeight() / tileH + 1][getWidth() / tileW + 1];
        for (int i = 0; i < tiles.length; i++)
        {
            for (int j = 0; j < tiles[i].length; j++)
            {
                tiles[i][j] = -1;
            }
        }
        for (int i = 0; i < tiles.length; i++)
        {
            for (int j = 0; j < tiles[i].length; j++)
            {
                int x = wang(i, j);
                tiles[i][j] = x;
                int[] p = indexToPoint(x);
                g.drawImage(tex, j * tileW, i * tileH, j * tileW + tileW, i * tileH + tileH, p[0] * tileW, p[1] * tileH, p[0] * tileW + tileW, p[1] * tileH + tileH, this);
                //g.drawRect(j*w, i*h, w, h);
            }
        }
    }

    public int[] indexToPoint(int x)
    {
        return new int[]
        {
            x % 4, x / 4
        };
    }

    /**
     * Calculates the Wang tile for the position.
     *
     * @param i
     * @param j
     * @return
     */
    public int wang(int i, int j)
    {
        int bits;
        do
        {
            bits = 0;
            // build integer out of bits that represent border colour
            for (int d = 0; d < 4; d++)
            {
                bits |= getBorder(i, j, d) << (3 - d);
            }
        } while (sameAsNeighbour(i, j, hash[bits]));
        return hash[bits];
    }

    /**
     * Checks if the given tile value is the same as it's neighbours and
     * diagonals. This is for avoiding repeats.
     *
     * @param i
     * @param j
     * @param x value of tile
     * @return
     */
    public boolean sameAsNeighbour(int i, int j, int x)
    {
        for (int d = 0; d < 4; d++)
        {
            if (get(i, j, directions[d]) == x)
            {
                return true;
            }
            if (get(i, j, diag[d]) == x)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Wraps the index a around length b.
     *
     * @param a
     * @param b
     * @return
     */
    public int wrap(int a, int b)
    {
        if (a >= b)
        {
            return a % b;
        }
        if (a < 0)
        {
            return a + b;
        }
        return a;
    }

    /**
     * Gets the value of the tile relative to the given indices.
     *
     * @param i
     * @param j
     * @param dir the relative direction
     * @return
     */
    public int get(int i, int j, int[] dir)
    {
        try
        {
            return tiles[i + dir[0]][j + dir[1]];
        } catch (ArrayIndexOutOfBoundsException ex)
        {
            return -1;
        }
    }

    /**
     * Gets the colour of the given edge.
     *
     * @param i
     * @param j
     * @param dir
     * @return
     */
    public int getBorder(int i, int j, int dir)
    {
        int x = get(i, j, directions[dir]);
        if (x < 0)
        {
            return random();
        }
        return borders[dir][x];
    }

    public int random()
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
