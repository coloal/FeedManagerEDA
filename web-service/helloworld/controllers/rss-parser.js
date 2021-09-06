let Parser = require('rss-parser');

class RssParser {
    constructor(){
        this.parser = new Parser();
    }
}

module.exports = new RssParser();