package backend.academy.linktracker.bot.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.scrapper.ScrapperClient;
import backend.academy.linktracker.bot.client.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.bot.client.scrapper.dto.LinkResponse;
import backend.academy.linktracker.bot.client.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.bot.client.scrapper.dto.RemoveLinkRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkTrackingServiceTest {

    @Mock
    private ScrapperClient scrapperClient;

    @InjectMocks
    private LinkTrackingService linkTrackingService;

    @Test
    void registerChat_delegatesToScrapperClient() {
        linkTrackingService.registerChat(123L);

        verify(scrapperClient).registerChat(123L);
    }

    @Test
    void deleteChat_delegatesToScrapperClient() {
        linkTrackingService.deleteChat(123L);

        verify(scrapperClient).deleteChat(123L);
    }

    @Test
    void getLinks_returnsResponseFromScrapperClient() {
        ListLinksResponse expected = new ListLinksResponse(List.of(), 0);

        when(scrapperClient.getLinks(123L)).thenReturn(expected);

        ListLinksResponse actual = linkTrackingService.getLinks(123L);

        assertSame(expected, actual);
        verify(scrapperClient).getLinks(123L);
    }

    @Test
    void addLink_wrapsArgumentsIntoAddLinkRequest() {
        LinkResponse expected =
                new LinkResponse(1L, "https://github.com/user/repo", List.of("java"), List.of("branch=main"));

        when(scrapperClient.addLink(org.mockito.Mockito.eq(123L), org.mockito.Mockito.any(AddLinkRequest.class)))
                .thenReturn(expected);

        LinkResponse actual = linkTrackingService.addLink(
                123L, "https://github.com/user/repo", List.of("java"), List.of("branch=main"));

        assertSame(expected, actual);

        ArgumentCaptor<AddLinkRequest> captor = ArgumentCaptor.forClass(AddLinkRequest.class);
        verify(scrapperClient).addLink(org.mockito.Mockito.eq(123L), captor.capture());

        AddLinkRequest request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("https://github.com/user/repo", request.link());
        org.junit.jupiter.api.Assertions.assertEquals(List.of("java"), request.tags());
        org.junit.jupiter.api.Assertions.assertEquals(List.of("branch=main"), request.filters());
    }

    @Test
    void removeLink_wrapsArgumentsIntoRemoveLinkRequest() {
        LinkResponse expected = new LinkResponse(1L, "https://github.com/user/repo", List.of(), List.of());

        when(scrapperClient.removeLink(org.mockito.Mockito.eq(123L), org.mockito.Mockito.any(RemoveLinkRequest.class)))
                .thenReturn(expected);

        LinkResponse actual = linkTrackingService.removeLink(123L, "https://github.com/user/repo");

        assertSame(expected, actual);

        ArgumentCaptor<RemoveLinkRequest> captor = ArgumentCaptor.forClass(RemoveLinkRequest.class);
        verify(scrapperClient).removeLink(org.mockito.Mockito.eq(123L), captor.capture());

        RemoveLinkRequest request = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("https://github.com/user/repo", request.link());
    }
}
