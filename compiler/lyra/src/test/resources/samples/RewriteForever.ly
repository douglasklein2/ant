class Application {
    def main {
        Int x = 0, y = 255;
        forever {
            x++;
            y--;
            if (x == 4) {
                break;
            }
        }
    }
}