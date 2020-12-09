package us.ihmc.humanoidBehaviors.exploreArea;

class LatticeCell
{
   private final int x, y;

   public LatticeCell(int x, int y)
   {
      this.x = x;
      this.y = y;
   }

   public LatticeCell(double x, double y)
   {
      this.x = ExploredAreaLattice.toIndex(x);
      this.y = ExploredAreaLattice.toIndex(y);
   }

   @Override
   public int hashCode()
   {
      return 13 * x + 17 * y;
   }

   public int getX()
   {
      return x;
   }

   public int getY()
   {
      return y;
   }

   @Override
   public boolean equals(Object other)
   {
      if (!(other instanceof LatticeCell))
      {
         return false;
      }

      LatticeCell otherCell = (LatticeCell) other;
      return (otherCell.x == x) && (otherCell.y == y);
   }
}
